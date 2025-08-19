package br.com.idhub.custody.service;

import br.com.idhub.custody.domain.StatusList;
import br.com.idhub.custody.domain.TransactionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;

@Service
public class BlockchainService {

    @Autowired
    private Web3j web3j;

    @Autowired
    private WalletService walletService;

    @Autowired
    private ContractService contractService;

    // Endereços dos contratos
    @Value("${blockchain.did-registry-address:0x8553c57aC9a666EAfC517Ffc4CF57e21d2D3a1cb}")
    private String didRegistryAddress;

    @Value("${blockchain.status-list-manager-address:0x93a284C91768F3010D52cD37f84f22c5052be40b}")
    private String statusListManagerAddress;

    @Value("${web3j.chain-id:1337}")
    private Long chainId;

    // Gas provider para zero gas (modo legacy)
    private final BigInteger gasPrice = BigInteger.ZERO;
    private final BigInteger gasLimit = BigInteger.valueOf(4_700_000);

    /**
     * Publicar StatusList no StatusListManager
     */
    public CompletableFuture<TransactionReceipt> publishStatusList(
            String listId,
            String uri,
            String hash,
            Long version,
            Long size,
            String purpose,
            String issuerWalletAddress) throws Exception {

        System.out.println("\n=== BLOCKCHAIN SERVICE - PUBLISH START ===");
        System.out.println("Parâmetros recebidos:");
        System.out.println("  - listId: '" + listId + "'");
        System.out.println("  - uri: '" + uri + "'");
        System.out.println("  - hash: '" + hash + "'");
        System.out.println("  - version: " + version);
        System.out.println("  - size: " + size);
        System.out.println("  - purpose: '" + purpose + "'");
        System.out.println("  - issuerWalletAddress: '" + issuerWalletAddress + "'");
        System.out.println("  - statusListManagerAddress: '" + statusListManagerAddress + "'");
        System.out.println("  - chainId: " + chainId);

        // Obter credenciais administrativas em vez das credenciais do emissor
        System.out.println("\n1. Obtendo credenciais administrativas...");
        Credentials credentials;
        try {
            credentials = walletService.getAdminCredentials();
            System.out.println("   Credenciais administrativas obtidas para address: " + credentials.getAddress());
        } catch (Exception e) {
            System.err.println("   ERRO ao obter credenciais administrativas: " + e.getMessage());
            throw e;
        }

        // Usar ContractService para gerar dados corretos da função
        System.out.println("\n2. Gerando dados da função...");
        String functionData;
        try {
            functionData = contractService.createListFunctionData(listId, uri, hash, size, purpose);
            System.out.println("   Dados da função gerados com sucesso");
            System.out.println("   Tamanho dos dados: " + functionData.length() + " chars");
            System.out.println("   Primeiros 100 chars: " + functionData.substring(0, Math.min(100, functionData.length())));
        } catch (Exception e) {
            System.err.println("   ERRO ao gerar dados da função: " + e.getMessage());
            throw e;
        }

        System.out.println("\n3. Enviando transação...");
        return sendTransaction(credentials, functionData, statusListManagerAddress);
    }

    /**
     * Atualizar StatusList existente
     */
    public CompletableFuture<TransactionReceipt> updateStatusList(
            String listId,
            Long newVersion,
            String newUri,
            String newHash,
            String issuerWalletAddress) throws Exception {

        System.out.println("\n=== BLOCKCHAIN SERVICE - UPDATE STATUS LIST START ===");
        System.out.println("Parâmetros recebidos:");
        System.out.println("  - listId: '" + listId + "'");
        System.out.println("  - newVersion: " + newVersion);
        System.out.println("  - newUri: '" + newUri + "'");
        System.out.println("  - newHash: '" + newHash + "'");
        System.out.println("  - issuerWalletAddress: '" + issuerWalletAddress + "'");

        // Usar credenciais administrativas (mesmas usadas na criação da StatusList)
        System.out.println("\n1. Obtendo credenciais administrativas...");
        Credentials credentials;
        try {
            credentials = walletService.getAdminCredentials();
            System.out.println("   Credenciais administrativas obtidas para address: " + credentials.getAddress());
        } catch (Exception e) {
            System.err.println("   ERRO ao obter credenciais administrativas: " + e.getMessage());
            throw e;
        }

        // Usar ContractService para gerar dados corretos da função
        System.out.println("\n2. Gerando dados da função publish...");
        String functionData;
        try {
            functionData = contractService.publishFunctionData(listId, newVersion, newUri, newHash);
            System.out.println("   Dados da função gerados com sucesso");
            System.out.println("   Tamanho dos dados: " + functionData.length() + " chars");
            System.out.println("   Primeiros 100 chars: " + functionData.substring(0, Math.min(100, functionData.length())));
        } catch (Exception e) {
            System.err.println("   ERRO ao gerar dados da função: " + e.getMessage());
            throw e;
        }

        System.out.println("\n3. Enviando transação de atualização...");
        return sendTransaction(credentials, functionData, statusListManagerAddress);
    }

    /**
     * Ancorar metadados de credencial no DIDRegistry
     */
    public CompletableFuture<TransactionReceipt> anchorCredentialMetadata(
            String issuerWalletAddress,
            String credentialId,
            String metadataHash,
            Long validTo) throws Exception {

        Credentials credentials = walletService.getWalletCredentialsForBlockchain(issuerWalletAddress);

        // Usar ContractService para gerar dados corretos da função
        String functionData = contractService.setAttributeFunctionData(
            issuerWalletAddress, credentialId, metadataHash, validTo
        );

        return sendTransaction(credentials, functionData, didRegistryAddress);
    }

    /**
     * Verificar se uma wallet tem ISSUER_ROLE em ambos os contratos
     */
    public boolean hasIssuerRole(String walletAddress) {
        try {
            System.out.println("\n=== VERIFICANDO ISSUER_ROLE ===");
            System.out.println("Wallet Address: " + walletAddress);

            // Verificar no StatusListManager
            System.out.println("Contract Address (StatusListManager): " + statusListManagerAddress);
            boolean hasRoleInStatusList = hasIssuerRoleForContract(walletAddress, statusListManagerAddress);

            // Verificar no DIDRegistry
            System.out.println("Contract Address (DIDRegistry): " + didRegistryAddress);
            boolean hasRoleInDIDRegistry = hasIssuerRoleForContract(walletAddress, didRegistryAddress);

            // Retorna true apenas se tiver a role em ambos os contratos
            return hasRoleInStatusList && hasRoleInDIDRegistry;

        } catch (Exception e) {
            System.out.println("Erro ao verificar ISSUER_ROLE: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Método auxiliar para verificar ISSUER_ROLE em um contrato específico
     */
    public boolean hasIssuerRoleForContract(String walletAddress, String contractAddress) {
        try {
            // Primeiro, obter o valor da constante ISSUER_ROLE do contrato
            String issuerRoleData = contractService.getIssuerRoleFunctionData();
            System.out.println("ISSUER_ROLE function data: " + issuerRoleData);

            // Fazer chamada para obter o valor de ISSUER_ROLE
            org.web3j.protocol.core.methods.request.Transaction issuerRoleTransaction =
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                    null, contractAddress, issuerRoleData);

            org.web3j.protocol.core.methods.response.EthCall issuerRoleResponse =
                web3j.ethCall(issuerRoleTransaction, DefaultBlockParameterName.LATEST).send();

            if (issuerRoleResponse.hasError()) {
                System.out.println("Erro ao obter ISSUER_ROLE: " + issuerRoleResponse.getError().getMessage());
                return false;
            }

            String issuerRoleValue = issuerRoleResponse.getValue();
            System.out.println("ISSUER_ROLE value: " + issuerRoleValue);

            // Verificar se o valor retornado é válido
            if (issuerRoleValue == null || issuerRoleValue.equals("0x")) {
                System.out.println("Valor ISSUER_ROLE inválido ou vazio");
                return false;
            }

            // Agora verificar se a wallet tem essa role usando hasRole
            String hasRoleData = contractService.getHasRoleFunctionData(issuerRoleValue, walletAddress);
            System.out.println("hasRole function data: " + hasRoleData);

            // Fazer chamada para hasRole
            org.web3j.protocol.core.methods.request.Transaction hasRoleTransaction =
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                    null, contractAddress, hasRoleData);

            org.web3j.protocol.core.methods.response.EthCall hasRoleResponse =
                web3j.ethCall(hasRoleTransaction, DefaultBlockParameterName.LATEST).send();

            if (hasRoleResponse.hasError()) {
                System.out.println("Erro ao verificar hasRole: " + hasRoleResponse.getError().getMessage());
                return false;
            }

            String hasRoleResult = hasRoleResponse.getValue();
            System.out.println("hasRole result: " + hasRoleResult);

            // Verificar se o resultado é válido
            if (hasRoleResult == null || hasRoleResult.equals("0x")) {
                System.out.println("Resultado hasRole inválido");
                return false;
            }

            // Decodificar o resultado boolean
            boolean hasRole = !"0x0000000000000000000000000000000000000000000000000000000000000000".equals(hasRoleResult) &&
                             hasRoleResult.endsWith("1");

            System.out.println("Wallet tem ISSUER_ROLE no contrato " + contractAddress + ": " + hasRole);
            return hasRole;

        } catch (Exception e) {
            System.out.println("Erro ao verificar ISSUER_ROLE no contrato " + contractAddress + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verificar se uma wallet tem DEFAULT_ADMIN_ROLE
     */
    public boolean hasDefaultAdminRole(String walletAddress) {
        try {
            System.out.println("\n=== VERIFICANDO DEFAULT_ADMIN_ROLE ===");
            System.out.println("Wallet Address: " + walletAddress);
            System.out.println("Contract Address: " + statusListManagerAddress);

            // DEFAULT_ADMIN_ROLE é sempre 0x00 (32 bytes de zeros)
            String defaultAdminRoleValue = "0x0000000000000000000000000000000000000000000000000000000000000000";
            System.out.println("DEFAULT_ADMIN_ROLE value: " + defaultAdminRoleValue);

            // Verificar se a wallet tem DEFAULT_ADMIN_ROLE usando hasRole
            String hasRoleData = contractService.getHasRoleFunctionData(defaultAdminRoleValue, walletAddress);
            System.out.println("hasRole function data: " + hasRoleData);

            // Fazer chamada para hasRole
            org.web3j.protocol.core.methods.request.Transaction hasRoleTransaction =
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                    null, statusListManagerAddress, hasRoleData);

            org.web3j.protocol.core.methods.response.EthCall hasRoleResponse =
                web3j.ethCall(hasRoleTransaction, DefaultBlockParameterName.LATEST).send();

            if (hasRoleResponse.hasError()) {
                System.out.println("Erro ao verificar hasRole: " + hasRoleResponse.getError().getMessage());
                return false;
            }

            String hasRoleResult = hasRoleResponse.getValue();
            System.out.println("hasRole result: " + hasRoleResult);

            // Verificar se o resultado é válido
            if (hasRoleResult == null || hasRoleResult.equals("0x")) {
                System.out.println("Resultado hasRole inválido");
                return false;
            }

            // Decodificar o resultado boolean
            // Em Solidity, um boolean true é representado como 0x0000...0001 (32 bytes)
            // e false como 0x0000...0000 (32 bytes)
            boolean hasRole = !"0x0000000000000000000000000000000000000000000000000000000000000000".equals(hasRoleResult) &&
                             hasRoleResult.endsWith("1");

            System.out.println("Wallet tem DEFAULT_ADMIN_ROLE: " + hasRole);
            return hasRole;

        } catch (Exception e) {
            System.out.println("Erro ao verificar DEFAULT_ADMIN_ROLE: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    // Método para enviar transação
    private CompletableFuture<TransactionReceipt> sendTransaction(
            Credentials credentials, String functionData, String contractAddress) throws Exception {

        System.out.println("\n=== SEND TRANSACTION - START ===");
        System.out.println("Parâmetros:");
        System.out.println("  - from: " + credentials.getAddress());
        System.out.println("  - to: " + contractAddress);
        System.out.println("  - chainId: " + chainId);
        System.out.println("  - gasPrice: " + gasPrice);
        System.out.println("  - gasLimit: " + gasLimit);
        System.out.println("  - functionData length: " + functionData.length());

        // Obter nonce
        System.out.println("\n1. Obtendo nonce...");
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
            credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();
        System.out.println("   Nonce obtido: " + nonce);

        // Criar transação raw (modo legacy, zero gas)
        System.out.println("\n2. Criando transação raw...");
        RawTransaction rawTransaction = RawTransaction.createTransaction(
            nonce,
            gasPrice,  // gasPrice = 0
            gasLimit,  // gasLimit = 4.7M
            contractAddress,  // to (endereço do contrato)
            BigInteger.ZERO,  // value
            functionData
        );
        System.out.println("   Transação raw criada");

        // Assinar transação
        System.out.println("\n3. Assinando transação...");
        byte[] signedMessage = org.web3j.crypto.TransactionEncoder.signMessage(rawTransaction, chainId, credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        System.out.println("   Transação assinada");
        System.out.println("   Hex length: " + hexValue.length());
        System.out.println("   Hex (primeiros 100 chars): " + hexValue.substring(0, Math.min(100, hexValue.length())));

        // Enviar transação
        System.out.println("\n4. Enviando transação...");
        return web3j.ethSendRawTransaction(hexValue).sendAsync()
            .thenCompose(response -> {
                if (response.hasError()) {
                    throw new RuntimeException("Erro na transação: " + response.getError().getMessage());
                }
                String txHash = response.getTransactionHash();
                return waitForTransactionReceipt(txHash, 30, 2000);
            });
    }


    /**
     * Conceder ISSUER_ROLE para uma wallet (usando chave administrativa configurada)
     */
    public CompletableFuture<TransactionReceipt> grantIssuerRole(String targetWalletAddress) throws Exception {
        try {
            System.out.println("\n=== CONCEDENDO ISSUER_ROLE ===");
            System.out.println("Target Wallet: " + targetWalletAddress);

            // Conceder role no StatusListManager
            System.out.println("Contract Address (StatusListManager): " + statusListManagerAddress);
            CompletableFuture<TransactionReceipt> statusListManagerFuture = grantIssuerRoleForContract(targetWalletAddress, statusListManagerAddress);

            // Aguardar a primeira transação ser confirmada
            TransactionReceipt statusListReceipt = statusListManagerFuture.get();
            if (!statusListReceipt.isStatusOK()) {
                throw new RuntimeException("Falha ao conceder ISSUER_ROLE no StatusListManager. Status: " + statusListReceipt.getStatus());
            }

            // Conceder role no DIDRegistry
            System.out.println("Contract Address (DIDRegistry): " + didRegistryAddress);
            CompletableFuture<TransactionReceipt> didRegistryFuture = grantIssuerRoleForContract(targetWalletAddress, didRegistryAddress);

            // Aguardar a segunda transação
            return didRegistryFuture;
        } catch (Exception e) {
            System.out.println("Erro ao conceder ISSUER_ROLE: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Método auxiliar para conceder ISSUER_ROLE em um contrato específico
     */
    private CompletableFuture<TransactionReceipt> grantIssuerRoleForContract(String targetWalletAddress, String contractAddress) throws Exception {
        // Primeiro, obter o valor da constante ISSUER_ROLE do contrato
        String issuerRoleData = contractService.getIssuerRoleFunctionData();

        // Fazer chamada para obter o valor de ISSUER_ROLE
        org.web3j.protocol.core.methods.request.Transaction issuerRoleTransaction =
            org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                null, contractAddress, issuerRoleData);

        org.web3j.protocol.core.methods.response.EthCall issuerRoleResponse =
            web3j.ethCall(issuerRoleTransaction, DefaultBlockParameterName.LATEST).send();

        if (issuerRoleResponse.hasError()) {
            throw new RuntimeException("Erro ao obter ISSUER_ROLE: " + issuerRoleResponse.getError().getMessage());
        }

        String issuerRoleValue = issuerRoleResponse.getValue();
        System.out.println("ISSUER_ROLE value: " + issuerRoleValue);

        // Verificar se o valor retornado é válido
        if (issuerRoleValue == null || issuerRoleValue.equals("0x")) {
            throw new RuntimeException("Valor ISSUER_ROLE inválido ou vazio");
        }

        // Gerar dados da função grantRole
        String grantRoleData = contractService.getGrantRoleFunctionData(issuerRoleValue, targetWalletAddress);
        System.out.println("grantRole function data: " + grantRoleData);

        // Obter credenciais administrativas
        Credentials adminCredentials = walletService.getAdminCredentials();
        System.out.println("Admin Address: " + adminCredentials.getAddress());

        // Enviar transação
        return sendTransaction(adminCredentials, grantRoleData, contractAddress);
    }

    /**
     * Conceder ISSUER_ROLE para uma wallet (método legado com adminWalletAddress)
     */
    public CompletableFuture<TransactionReceipt> grantIssuerRole(String targetWalletAddress, String adminWalletAddress) throws Exception {
        // Usar o novo método que utiliza a chave administrativa configurada
        return grantIssuerRole(targetWalletAddress);
    }
    /**
     * Aguardar receipt da transação com retry
     */
    private CompletableFuture<TransactionReceipt> waitForTransactionReceipt(String txHash, int maxAttempts, long intervalMs) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("\n=== AGUARDANDO RECEIPT ===");
            System.out.println("TX Hash: " + txHash);

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    System.out.println("\nTentativa " + attempt + "/" + maxAttempts + "...");

                    Optional<TransactionReceipt> receiptResponse = web3j
                        .ethGetTransactionReceipt(txHash)
                        .send()
                        .getTransactionReceipt();

                    if (receiptResponse.isPresent()) {
                        TransactionReceipt receipt = receiptResponse.get();
                        System.out.println("\n=== RECEIPT ENCONTRADO ===");
                        System.out.println("  - TX Hash: " + receipt.getTransactionHash());
                        System.out.println("  - Block Number: " + receipt.getBlockNumber());
                        System.out.println("  - Gas Used: " + receipt.getGasUsed());
                        System.out.println("  - Status: " + receipt.getStatus());
                        System.out.println("  - Contract Address: " + receipt.getContractAddress());

                        // VERIFICAR STATUS DA TRANSAÇÃO
                        if ("0x0".equals(receipt.getStatus())) {
                            System.err.println("\n❌ TRANSAÇÃO FALHOU!");
                            System.err.println("Status: " + receipt.getStatus());
                            System.err.println("Gas Used: " + receipt.getGasUsed());

                            // Tentar obter mais detalhes do erro
                            String errorReason = "Transação falhou na execução";
                            try {
                                // Verificar logs de eventos para mais detalhes
                                if (receipt.getLogs() != null && !receipt.getLogs().isEmpty()) {
                                    System.err.println("Logs da transação:");
                                    receipt.getLogs().forEach(log -> {
                                        System.err.println("  - Address: " + log.getAddress());
                                        System.err.println("  - Topics: " + log.getTopics());
                                        System.err.println("  - Data: " + log.getData());
                                    });
                                } else {
                                    System.err.println("Nenhum log encontrado na transação falhada");
                                }
                            } catch (Exception e) {
                                System.err.println("Erro ao analisar logs: " + e.getMessage());
                            }

                            throw new RuntimeException("Transação falhou na blockchain. TX: " + txHash +
                                                         ". Status: " + receipt.getStatus() +
                                                         ". Gas usado: " + receipt.getGasUsed() +
                                                         ". Motivo: " + errorReason);
                        }

                        System.out.println("✅ Transação executada com sucesso!");
                        return receipt;
                    } else {
                        System.out.println("   Receipt ainda não disponível...");
                    }

                    if (attempt < maxAttempts) {
                        Thread.sleep(intervalMs);
                    }

                } catch (Exception e) {
                    System.err.println("   Erro na tentativa " + attempt + ": " + e.getMessage());
                    if (attempt == maxAttempts) {
                        throw new RuntimeException("Erro ao obter receipt para TX: " + txHash, e);
                    }

                    try {
                        Thread.sleep(intervalMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrompido aguardando receipt", ie);
                    }
                }
            }

            throw new RuntimeException("Receipt não encontrado para TX: " + txHash);
        });
    }

        /**
     * Retorna o endereço do contrato StatusListManager
     */
    public String getStatusListManagerAddress() {
        return statusListManagerAddress;
    }

    /**
     * Retorna o endereço do contrato DIDRegistry
     */
    public String getDidRegistryAddress() {
        return didRegistryAddress;
    }

    /**
     * Revogar atributo no DIDRegistry
     */
    public CompletableFuture<TransactionReceipt> revokeAttribute(
            String issuerWalletAddress,
            String credentialId,
            String metadataHash) throws Exception {

        Credentials credentials = walletService.getWalletCredentialsForBlockchain(issuerWalletAddress);

        // Usar ContractService para gerar dados corretos da função
        String functionData = contractService.revokeAttributeFunctionData(
            issuerWalletAddress, credentialId, metadataHash
        );

        return sendTransaction(credentials, functionData, didRegistryAddress);
    }
}

