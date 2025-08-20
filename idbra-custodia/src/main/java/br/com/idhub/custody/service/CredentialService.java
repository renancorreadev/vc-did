package br.com.idhub.custody.service;

import br.com.idhub.custody.domain.*;
import br.com.idhub.custody.repository.CredentialRepository;
import br.com.idhub.custody.repository.StatusListRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class CredentialService {

    @Autowired
    private WalletService walletService;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private StatusListRepository statusListRepository;

    @Autowired
    private CryptoService cryptoService;

    @Autowired
    private StatusListService statusListService;

    @Autowired
    private BlockchainService blockchainService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Criar e assinar uma credencial verificável
     */
    public String createCredential(CredentialRequest request) throws Exception {
        // 1. Validar dados da credencial
        validateCredentialRequest(request);

        // 2. Gerar ID único para a credencial
        String credentialId = generateCredentialId();

        // 3. Obter credenciais da wallet do emissor
        Credentials issuerCredentials = walletService.getWalletCredentialsWithMasterPassword(request.getIssuerWalletAddress());

        // 4. Gerar payload da credencial
        Map<String, Object> credentialPayload = buildCredentialPayload(request, credentialId);

        // 5. Assinar com JWT/JWS
        String jwsToken = signCredential(credentialPayload, issuerCredentials);

        // 6. Criar entidade Credential
        Credential credential = new Credential(
            credentialId,
            request.getIssuerDid(),
            request.getHolderDid(),
            objectMapper.writeValueAsString(credentialPayload),
            request.getStatusListId(),
            getNextStatusListIndex(request.getStatusListId())
        );
        credential.setJwsToken(jwsToken);
        credential.setIssuerWalletAddress(request.getIssuerWalletAddress());
        credential.setExpiresAt(request.getExpiresAt());

        // Registrar credencial no blockchain ANTES de salvar no banco
        try {
            String credentialData = objectMapper.writeValueAsString(credentialPayload);
            String credentialHash = calculateHash(credentialData);

            // ✅ VALIDAR PARÂMETROS OBRIGATÓRIOS
            if (request.getHolderWalletAddress() == null || request.getHolderWalletAddress().trim().isEmpty()) {
                throw new RuntimeException("HolderWalletAddress é obrigatório para registrar credencial no blockchain");
            }

            System.out.println("Registrando credencial no blockchain: " + credentialId);
            System.out.println("Subject: " + request.getHolderWalletAddress());
            System.out.println("Hash: " + credentialHash);

            // Aguardar confirmação da transação blockchain
            TransactionReceipt receipt = blockchainService.issueCredential(
                credentialId,
                request.getHolderWalletAddress(),
                credentialHash
            ).get();

            if (!receipt.isStatusOK()) {
                // ✅ FALHAR A OPERAÇÃO SE BLOCKCHAIN FALHAR
                throw new RuntimeException("Falha ao registrar credencial no contrato: " + credentialId +
                                         " - TX: " + receipt.getTransactionHash() +
                                         " - Status: " + receipt.getStatus());
            } else {
                System.out.println("✅ Credencial registrada no contrato: " + credentialId +
                                 " - TX: " + receipt.getTransactionHash());
            }
        } catch (Exception e) {
            System.err.println("❌ ERRO CRÍTICO ao registrar credencial no blockchain: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Falha crítica ao registrar credencial no blockchain. Credencial não será criada: " + e.getMessage(), e);
        }

        Credential savedCredential = credentialRepository.save(credential);

        // Ancorar metadados no DIDRegistry (opcional, para did:ethr)
        try {
            String credentialData = objectMapper.writeValueAsString(credentialPayload);
            String metadataHash = calculateHash(credentialData);
            Long validTo = request.getExpiresAt() != null ?
                request.getExpiresAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond() :
                LocalDateTime.now().plusYears(10).atZone(java.time.ZoneId.systemDefault()).toEpochSecond();

            blockchainService.anchorCredentialMetadata(
                request.getIssuerWalletAddress(),
                credentialId,
                metadataHash,
                validTo
            ).thenAccept(receipt -> {
                // Log de sucesso
                System.out.println("Metadados ancorados no DIDRegistry: " + credentialId +
                                 " - TX: " + receipt.getTransactionHash());
            }).exceptionally(throwable -> {
                // Log de erro
                System.err.println("Erro ao ancorar metadados: " + credentialId +
                                 " - " + throwable.getMessage());
                return null;
            });
        } catch (Exception e) {
            // Log de erro mas não falha a operação
            System.err.println("Erro ao ancorar metadados: " + e.getMessage());
        }

        return jwsToken;
    }

    /**
     * Verificar uma credencial
     */
    public CredentialVerification verifyCredential(String jwsToken) throws Exception {
        CredentialVerification verification = new CredentialVerification(false, "");

        try {
            // 1. Decodificar JWT/JWS
            Map<String, Object> claims = decodeJwsToken(jwsToken);

            // 2. Extrair dados da credencial
            String credentialId = (String) claims.get("jti");
            String issuerDid = (String) claims.get("iss");
            String holderDid = (String) claims.get("sub");

            verification.setCredentialId(credentialId);
            verification.setIssuerDid(issuerDid);
            verification.setHolderDid(holderDid);

            // 3. Verificar se existe no banco
            Optional<Credential> credentialOpt = credentialRepository.findByCredentialId(credentialId);
            if (credentialOpt.isEmpty()) {
                verification.addError("Credencial não encontrada no banco");
                return verification;
            }

            Credential credential = credentialOpt.get();
            verification.setStatus(credential.getStatus());
            verification.setIssuedAt(credential.getIssuedAt());
            verification.setExpiresAt(credential.getExpiresAt());

            // 4. Verificar assinatura (simplificado - em produção usar biblioteca JWT)
            if (verifySignature(jwsToken, credential)) {
                verification.setValid(true);
            } else {
                verification.addError("Assinatura inválida");
            }

            // 5. Verificar status de revogação
            if ("REVOKED".equals(credential.getStatus())) {
                verification.setValid(false);
                verification.addError("Credencial revogada");
            } else {
                // Verificar StatusList on-chain
                try {
                    boolean isRevoked = statusListService.isCredentialRevoked(
                        credential.getStatusListId(),
                        credential.getStatusListIndex()
                    );
                    if (isRevoked) {
                        verification.setValid(false);
                        verification.addError("Credencial revogada na StatusList");
                    }
                } catch (Exception e) {
                    verification.addWarning("Não foi possível verificar StatusList: " + e.getMessage());
                }
            }

            // 6. Verificar expiração
            if (credential.getExpiresAt() != null && credential.getExpiresAt().isBefore(LocalDateTime.now())) {
                verification.setValid(false);
                verification.addError("Credencial expirada");
            }

        } catch (Exception e) {
            verification.addError("Erro na verificação: " + e.getMessage());
        }

        return verification;
    }

    /**
     * Revogar uma credencial
     */
    public boolean revokeCredential(String credentialId) throws Exception {
        Optional<Credential> credentialOpt = credentialRepository.findByCredentialId(credentialId);
        if (credentialOpt.isEmpty()) {
            throw new RuntimeException("Credencial não encontrada: " + credentialId);
        }

        Credential credential = credentialOpt.get();
        credential.setStatus("REVOKED");
        credentialRepository.save(credential);

        // Atualizar StatusList on-chain
        statusListService.revokeCredentialInList(
            credential.getStatusListId(),
            credential.getStatusListIndex()
        );

        return true;
    }

    /**
     * Criar ou atualizar StatusList
     */
    public StatusList createOrUpdateStatusList(String listId, String uri, String purpose,
                                            String issuer, String issuerWalletAddress) throws Exception {

        if (statusListRepository.existsByListId(listId)) {
            // Atualizar existente
            return statusListService.updateStatusList(listId, uri, issuerWalletAddress);
        } else {
            // Criar nova
            return statusListService.createStatusList(listId, uri, purpose, issuer, issuerWalletAddress);
        }
    }

    // Métodos de consulta para Credential

    public List<Credential> getAllCredentials() {
        return credentialRepository.findAll();
    }

    public Optional<Credential> getCredentialById(String credentialId) {
        return credentialRepository.findByCredentialId(credentialId);
    }

    public List<Credential> getCredentialsByIssuer(String issuerDid) {
        return credentialRepository.findByIssuerDid(issuerDid);
    }

    public List<Credential> getCredentialsByHolder(String holderDid) {
        return credentialRepository.findByHolderDid(holderDid);
    }

    public List<Credential> getCredentialsByStatus(String status) {
        return credentialRepository.findByStatus(status);
    }

    // Métodos de consulta para StatusList

    public Optional<StatusList> getStatusListById(String listId) {
        return statusListRepository.findByListId(listId);
    }

    public List<StatusList> getStatusListsByIssuer(String issuer) {
        return statusListRepository.findByIssuer(issuer);
    }

    public List<StatusList> getStatusListsByPurpose(String purpose) {
        return statusListRepository.findByPurpose(purpose);
    }

    public Optional<StatusList> getLatestStatusListVersion(String listId) {
        return statusListRepository.findLatestVersionByListId(listId);
    }

    // Métodos privados auxiliares

    private void validateCredentialRequest(CredentialRequest request) {
        if (request.getIssuerDid() == null || request.getIssuerDid().trim().isEmpty()) {
            throw new RuntimeException("Issuer DID é obrigatório");
        }
        if (request.getHolderDid() == null || request.getHolderDid().trim().isEmpty()) {
            throw new RuntimeException("Holder DID é obrigatório");
        }
        if (request.getCredentialType() == null || request.getCredentialType().trim().isEmpty()) {
            throw new RuntimeException("Tipo de credencial é obrigatório");
        }
        if (request.getStatusListId() == null || request.getStatusListId().trim().isEmpty()) {
            throw new RuntimeException("StatusList ID é obrigatório");
        }
    }

    private String generateCredentialId() {
        return "urn:uuid:" + UUID.randomUUID().toString();
    }

    private Map<String, Object> buildCredentialPayload(CredentialRequest request, String credentialId) {
        Map<String, Object> payload = new HashMap<>();

        // Header padrão
        payload.put("iss", request.getIssuerDid());
        payload.put("sub", request.getHolderDid());
        payload.put("jti", credentialId);
        payload.put("iat", System.currentTimeMillis() / 1000);
        payload.put("exp", request.getExpiresAt() != null ?
            request.getExpiresAt().atZone(ZoneId.systemDefault()).toEpochSecond() :
            (System.currentTimeMillis() / 1000) + 365 * 24 * 60 * 60); // 1 ano padrão

        // Tipo da credencial
        payload.put("vc", Map.of(
            "@context", Arrays.asList(
                "https://www.w3.org/2018/credentials/v1",
                "https://www.w3.org/2018/credentials/examples/v1"
            ),
            "type", Arrays.asList("VerifiableCredential", request.getCredentialType()),
            "credentialSubject", request.getCredentialSubject(),
            "credentialStatus", Map.of(
                "id", "https://idbra.example/status/" + request.getStatusListId() + ".json#" + getNextStatusListIndex(request.getStatusListId()),
                "type", "StatusList2021Entry",
                "statusPurpose", "revocation",
                "statusListIndex", getNextStatusListIndex(request.getStatusListId()),
                "statusListCredential", "https://idbra.example/status/" + request.getStatusListId() + ".json"
            )
        ));

        return payload;
    }

    private String signCredential(Map<String, Object> payload, Credentials issuerCredentials) throws Exception {
        // Obter chave privada
        String privateKeyHex = issuerCredentials.getEcKeyPair().getPrivateKey().toString(16);
        if (!privateKeyHex.startsWith("0x")) {
            privateKeyHex = "0x" + privateKeyHex;
        }

        // Para simplificar, vamos usar uma assinatura simulada por enquanto
        // TODO: Implementar assinatura JWT real com ES256
        return "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9." +
               java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                   objectMapper.writeValueAsBytes(payload)
               ) + ".simulated_signature";
    }

    private Map<String, Object> decodeJwsToken(String jwsToken) throws Exception {
        try {
            // Decodificar o JWT sem verificar a assinatura (apenas para extrair claims)
            String[] parts = jwsToken.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("JWT inválido - deve ter 3 partes");
            }

            // Decodificar o payload (segunda parte)
            String payload = parts[1];
            byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
            String payloadJson = new String(decodedBytes);

            // Converter JSON para Map
            return objectMapper.readValue(payloadJson, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao decodificar JWT: " + e.getMessage(), e);
        }
    }

    private boolean verifySignature(String jwsToken, Credential credential) {
        // TODO: Implementar verificação de assinatura adequada
        return true; // Simulado
    }

    private Integer getNextStatusListIndex(String statusListId) {
        try {
            Long count = statusListRepository.countByListId(statusListId);
            return count.intValue();
        } catch (Exception e) {
            // Se não conseguir contar, retorna 0
            return 0;
        }
    }

    private String calculateHash(String data) throws Exception {
        // Calcular hash SHA-256 real
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes("UTF-8"));

            // Garantir que sempre temos 32 bytes
            if (hashBytes.length != 32) {
                throw new RuntimeException("SHA-256 deve produzir exatamente 32 bytes, mas produziu: " + hashBytes.length);
            }

            // Converter para hex com prefixo 0x
            String hexHash = "0x" + org.web3j.utils.Numeric.toHexStringNoPrefix(hashBytes);
            System.out.println("[DEBUG] Hash calculado: " + hexHash + " (length: " + hexHash.length() + ")");

            return hexHash;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao calcular hash", e);
        }
    }

    /**
     * Extrair credentialId de um JWT
     */
    public String extractCredentialIdFromJWT(String jwsToken) throws Exception {
        try {
            Map<String, Object> claims = decodeJwsToken(jwsToken);
            return (String) claims.get("jti");
        } catch (Exception e) {
            throw new RuntimeException("Erro ao extrair credentialId do JWT: " + e.getMessage(), e);
        }
    }
}
