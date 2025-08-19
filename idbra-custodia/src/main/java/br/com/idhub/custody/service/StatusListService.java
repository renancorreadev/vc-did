package br.com.idhub.custody.service;

import br.com.idhub.custody.domain.*;
import br.com.idhub.custody.repository.CredentialRepository;
import br.com.idhub.custody.repository.StatusListRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatusListService {

    @Autowired
    private StatusListRepository statusListRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private BlockchainService blockchainService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Criar nova StatusList
     */
    public StatusList createStatusList(String listId, String uri, String purpose,
                                 String issuer, String issuerWalletAddress) throws Exception {
    // Verificar se já existe
    if (statusListRepository.existsByListId(listId)) {
        throw new RuntimeException("StatusList já existe: " + listId);
    }

    // Verificar se a wallet tem ISSUER_ROLE
    if (!blockchainService.hasIssuerRole(issuerWalletAddress)) {
        throw new RuntimeException("Wallet não tem ISSUER_ROLE: " + issuerWalletAddress);
    }

    // Gerar dados da lista
    StatusListData listData = generateStatusListData(listId, issuer, new ArrayList<>());
    String statusListJson = objectMapper.writeValueAsString(listData);

    // Calcular hash real
    String hash = calculateHash(statusListJson);

    // Criar entidade local
    StatusList statusList = new StatusList(listId, uri, hash, 1L, purpose, issuer);
    statusList.setIssuerWalletAddress(issuerWalletAddress);
    statusList.setStatusListData(statusListJson);

    // Publicar on-chain via smart contract e aguardar confirmação
    try {
        TransactionReceipt receipt = blockchainService.publishStatusList(
            listId,
            uri,
            hash,
            1L,
            (long) listData.getStatusList().size(),
            purpose,
            issuerWalletAddress
        ).get(); // Aguarda a confirmação da transação

        if ("0x0".equals(receipt.getStatus())) {
            throw new RuntimeException("Falha ao publicar StatusList na blockchain. TX: " +
                                     receipt.getTransactionHash());
        }

        // Salvar localmente apenas após confirmação na blockchain
        return statusListRepository.save(statusList);

    } catch (Exception e) {
        throw new RuntimeException("Erro ao publicar StatusList: " + e.getMessage(), e);
    }
}

    /**
     * Atualizar StatusList existente
     */
    public StatusList updateStatusList(String listId, String newUri, String issuerWalletAddress) throws Exception {

        Optional<StatusList> existingOpt = statusListRepository.findLatestVersionByListId(listId);
        if (existingOpt.isEmpty()) {
            throw new RuntimeException("StatusList não encontrada: " + listId);
        }

        StatusList existing = existingOpt.get();
        Long newVersion = existing.getVersion() + 1;

        // Gerar nova lista com status atualizados
        List<Integer> currentStatus = getCurrentStatusList(listId);
        StatusListData listData = generateStatusListData(listId, existing.getIssuer(), currentStatus);
        String statusListJson = objectMapper.writeValueAsString(listData);

        // Calcular hash real
        String hash = calculateHash(statusListJson);

        // Criar nova versão
        StatusList newStatusList = new StatusList(listId, newUri, hash, newVersion, existing.getPurpose(), existing.getIssuer());
        newStatusList.setIssuerWalletAddress(issuerWalletAddress);
        newStatusList.setStatusListData(statusListJson);

        // Publicar atualização on-chain via smart contract e aguardar confirmação
        try {
            TransactionReceipt receipt = blockchainService.updateStatusList(
                listId,
                newVersion,
                newUri,
                hash,
                issuerWalletAddress
            ).get(); // Aguarda a confirmação da transação

            if (!receipt.isStatusOK()) {
                throw new RuntimeException("Falha ao atualizar StatusList na blockchain. TX: " +
                                       receipt.getTransactionHash());
            }

            // Salvar localmente apenas após confirmação na blockchain
            return statusListRepository.save(newStatusList);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar StatusList na blockchain: " + e.getMessage(), e);
        }
    }

    /**
     * Revogar credencial na StatusList
     */
    public boolean revokeCredentialInList(String listId, Integer statusListIndex) throws Exception {
        System.out.println("[REVOKE] Iniciando revogação para listId: " + listId + ", index: " + statusListIndex);

        Optional<StatusList> latestOpt = statusListRepository.findLatestVersionByListId(listId);
        if (latestOpt.isEmpty()) {
            throw new RuntimeException("StatusList não encontrada: " + listId);
        }

        StatusList latest = latestOpt.get();
        System.out.println("[REVOKE] StatusList encontrada - versão atual: " + latest.getVersion());

        // Obter status atual
        List<Integer> currentStatus = getCurrentStatusList(listId);
        System.out.println("[REVOKE] Status atual obtido - tamanho: " + currentStatus.size());

        // Verificar se o índice existe
        if (statusListIndex >= currentStatus.size()) {
            System.out.println("[REVOKE] Expandindo lista de " + currentStatus.size() + " para " + (statusListIndex + 1));
            // Expandir lista se necessário
            while (currentStatus.size() <= statusListIndex) {
                currentStatus.add(0); // 0 = válido
            }
        }

        // Marcar como revogado (1 = revogado)
        currentStatus.set(statusListIndex, 1);
        System.out.println("[REVOKE] Credencial marcada como revogada no índice: " + statusListIndex);

        // Gerar nova versão
        StatusListData listData = generateStatusListData(listId, latest.getIssuer(), currentStatus);
        String statusListJson = objectMapper.writeValueAsString(listData);
        String hash = calculateHash(statusListJson);
        System.out.println("[REVOKE] Nova versão gerada - hash: " + hash);

        // Criar nova versão
        StatusList newStatusList = new StatusList(listId, latest.getUri(), hash, latest.getVersion() + 1,
                                                latest.getPurpose(), latest.getIssuer());
        newStatusList.setIssuerWalletAddress(latest.getIssuerWalletAddress());
        newStatusList.setStatusListData(statusListJson);
        System.out.println("[REVOKE] Nova StatusList criada - versão: " + newStatusList.getVersion());

        // 1. Primeiro revogar o atributo no DIDRegistry
        try {
            System.out.println("[REVOKE] Iniciando revogação do atributo no DIDRegistry...");
            // Obter credencial associada a este índice na StatusList
            Optional<Credential> credentialOpt = credentialRepository.findByStatusListIdAndStatusListIndex(listId, statusListIndex);
            if (credentialOpt.isPresent()) {
                Credential credential = credentialOpt.get();
                String credentialId = credential.getCredentialId();
                String issuerWalletAddress = latest.getIssuerWalletAddress();
                System.out.println("[REVOKE] Credencial encontrada: " + credentialId + ", issuer: " + issuerWalletAddress);

                // Revogar atributo no DIDRegistry
                TransactionReceipt receipt = blockchainService.revokeAttribute(
                    issuerWalletAddress,
                    credentialId,
                    hash
                ).get(); // Aguardar confirmação

                System.out.println("[REVOKE] Transação de revogação enviada - hash: " + receipt.getTransactionHash());
                System.out.println("[REVOKE] Status da transação de revogação: " + receipt.isStatusOK());

                if (!receipt.isStatusOK()) {
                    throw new RuntimeException("Falha ao revogar atributo no DIDRegistry");
                }
            } else {
                System.out.println("[REVOKE] AVISO: Credencial não encontrada para listId: " + listId + ", index: " + statusListIndex);
            }
        } catch (Exception e) {
            System.err.println("[REVOKE] ERRO ao revogar atributo no DIDRegistry: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erro ao revogar atributo no DIDRegistry: " + e.getMessage(), e);
        }

        // 2. Depois atualizar a StatusList na blockchain
        try {
            System.out.println("[REVOKE] Iniciando atualização da StatusList na blockchain...");
            System.out.println("[REVOKE] Parâmetros: listId=" + listId + ", version=" + newStatusList.getVersion() + ", uri=" + newStatusList.getUri() + ", hash=" + hash);

            TransactionReceipt receipt = blockchainService.updateStatusList(
                listId,
                newStatusList.getVersion(),
                newStatusList.getUri(),
                hash,
                latest.getIssuerWalletAddress()
            ).get(); // Aguardar confirmação

            System.out.println("[REVOKE] Transação de atualização da StatusList enviada - hash: " + receipt.getTransactionHash());
            System.out.println("[REVOKE] Status da transação de atualização: " + receipt.isStatusOK());
            System.out.println("[REVOKE] Gas usado: " + receipt.getGasUsed());

            if (!receipt.isStatusOK()) {
                throw new RuntimeException("Falha ao atualizar StatusList na blockchain");
            }

            System.out.println("[REVOKE] StatusList atualizada com sucesso na blockchain!");
        } catch (Exception e) {
            System.err.println("[REVOKE] ERRO ao atualizar StatusList na blockchain: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erro ao atualizar StatusList na blockchain: " + e.getMessage(), e);
        }

        // 3. Por fim, salvar a nova versão localmente
        System.out.println("[REVOKE] Salvando nova versão localmente...");
        statusListRepository.save(newStatusList);
        System.out.println("[REVOKE] Nova versão salva com sucesso! Versão: " + newStatusList.getVersion());

        return true;
    }

    /**
     * Verificar status de uma credencial
     */
    public boolean isCredentialRevoked(String listId, Integer statusListIndex) throws Exception {

        Optional<StatusList> latestOpt = statusListRepository.findLatestVersionByListId(listId);
        if (latestOpt.isEmpty()) {
            throw new RuntimeException("StatusList não encontrada: " + listId);
        }

        StatusList latest = latestOpt.get();
        List<Integer> currentStatus = getCurrentStatusList(listId);

        if (statusListIndex >= currentStatus.size()) {
            return false; // Índice não existe = válido
        }

        return currentStatus.get(statusListIndex) == 1; // 1 = revogado
    }

    /**
     * Obter StatusList como JSON
     */
    public String getStatusListAsJson(String listId) throws Exception {

        Optional<StatusList> latestOpt = statusListRepository.findLatestVersionByListId(listId);
        if (latestOpt.isEmpty()) {
            throw new RuntimeException("StatusList não encontrada: " + listId);
        }

        return latestOpt.get().getStatusListData();
    }

    /**
     * Obter metadados da StatusList
     */
    public Map<String, Object> getStatusListMetadata(String listId) throws Exception {

        Optional<StatusList> latestOpt = statusListRepository.findLatestVersionByListId(listId);
        if (latestOpt.isEmpty()) {
            throw new RuntimeException("StatusList não encontrada: " + listId);
        }

        StatusList latest = latestOpt.get();
        Map<String, Object> metadata = new HashMap<>();

        metadata.put("listId", latest.getListId());
        metadata.put("uri", latest.getUri());
        metadata.put("hash", latest.getHash());
        metadata.put("version", latest.getVersion());
        metadata.put("purpose", latest.getPurpose());
        metadata.put("issuer", latest.getIssuer());
        metadata.put("createdAt", latest.getCreatedAt());
        metadata.put("updatedAt", latest.getUpdatedAt());

        // Contar credenciais válidas e revogadas
        List<Integer> statusList = getCurrentStatusList(listId);
        long validCount = statusList.stream().filter(status -> status == 0).count();
        long revokedCount = statusList.stream().filter(status -> status == 1).count();

        metadata.put("totalCredentials", statusList.size());
        metadata.put("validCredentials", validCount);
        metadata.put("revokedCredentials", revokedCount);

        return metadata;
    }

    /**
     * Listar todas as StatusLists
     */
    public List<StatusList> getAllStatusLists() {
        return statusListRepository.findAll();
    }

    /**
     * Obter StatusList por ID
     */
    public Optional<StatusList> getStatusListById(String listId) {
        return statusListRepository.findByListId(listId);
    }

    // Métodos privados auxiliares

    private StatusListData generateStatusListData(String listId, String issuer, List<Integer> statusList) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

        StatusListData data = new StatusListData();
        data.setId("https://idbra.example/status/" + listId + ".json");
        data.setIssuer(issuer);
        data.setIssued(now);
        data.setValidFrom(now);
        data.setValidUntil(LocalDateTime.now().plusYears(10).format(DateTimeFormatter.ISO_DATE_TIME));
        data.setCredentialSubject(Map.of(
            "id", "https://idbra.example/status/" + listId + ".json",
            "type", "StatusList2021",
            "statusPurpose", "revocation",
            "encodedList", encodeStatusList(statusList)
        ).toString());
        data.setStatusList(statusList);

        return data;
    }

    private List<Integer> getCurrentStatusList(String listId) throws Exception {
        Optional<StatusList> latestOpt = statusListRepository.findLatestVersionByListId(listId);
        if (latestOpt.isEmpty()) {
            return new ArrayList<>();
        }

        StatusList latest = latestOpt.get();
        StatusListData data = objectMapper.readValue(latest.getStatusListData(), StatusListData.class);
        return data.getStatusList() != null ? data.getStatusList() : new ArrayList<>();
    }

    private String encodeStatusList(List<Integer> statusList) {
        // Codificar lista de status como base64 (simplificado)
        StringBuilder encoded = new StringBuilder();
        for (Integer status : statusList) {
            encoded.append(status == 1 ? "1" : "0");
        }
        return encoded.toString();
    }

    private String calculateHash(String data) {
        // Calcular hash SHA-256 real
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes("UTF-8"));
            return "0x" + Numeric.toHexString(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao calcular hash", e);
        }
    }
}
