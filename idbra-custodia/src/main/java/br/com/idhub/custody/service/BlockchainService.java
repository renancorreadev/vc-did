package br.com.idhub.custody.service;

import br.com.idhub.custody.domain.StatusList;
import br.com.idhub.custody.domain.TransactionResult;
import br.com.idhub.custody.domain.IdentityInfo;
import br.com.idhub.custody.domain.RevocationRecord;
import br.com.idhub.custody.domain.SystemMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import java.util.List;

@Service
public class BlockchainService {

    @Autowired
    private Web3j web3j;

    @Autowired
    private WalletService walletService;

    @Autowired
    private ContractService contractService;

    @Value("${blockchain.did-registry-address:0xc47a675198759Cf712a53Bb4a7EDbC33bb799285}")
    private String didRegistryAddress;

    // Remover esta linha completamente:
    // @Value("${blockchain.status-list-manager-address:0x93a284C91768F3010D52cD37f84f22c5052be40b}")
    // private String statusListManagerAddress;

    @Value("${web3j.chain-id:1337}")
    private Long chainId;

    // Gas configuration
    private final BigInteger gasPrice = BigInteger.ZERO;
    private final BigInteger gasLimit = BigInteger.valueOf(4_700_000);

    /**
     * Publish a new status list to the blockchain
     */
    public CompletableFuture<TransactionReceipt> publishStatusList(
            String listId,
            String uri,
            String hash,
            Long version,
            Long size,
            String purpose,
            String issuerWalletAddress) throws Exception {

        String functionData = contractService.publishStatusListFunctionData(
                issuerWalletAddress, listId, uri, hash
        );

        Credentials credentials = walletService.getWalletCredentialsForBlockchain(issuerWalletAddress);
        return sendTransaction(credentials, functionData, didRegistryAddress);
    }

    /**
     * Update an existing status list
     */
    public CompletableFuture<TransactionReceipt> updateStatusList(
            String listId,
            Long newVersion,
            String newUri,
            String newHash,
            String issuerWalletAddress) throws Exception {

        // Usar publishStatusListFunctionData em vez de createListFunctionData
        String functionData = contractService.publishStatusListFunctionData(
                issuerWalletAddress, listId, newUri, newHash
        );

        Credentials credentials = walletService.getWalletCredentialsForBlockchain(issuerWalletAddress);
        return sendTransaction(credentials, functionData, didRegistryAddress);
    }



    /**
     * Check if a wallet has issuer role - versão simplificada usando valor constante
     */
    public CompletableFuture<TransactionReceipt> anchorCredentialMetadata(
            String issuerWalletAddress,
            String credentialId,
            String metadataHash,
            Long validTo) throws Exception {

        String functionData = contractService.setAttributeFunctionData(
                credentialId, "credentialMetadata", metadataHash, validTo
        );

        Credentials credentials = walletService.getWalletCredentialsForBlockchain(issuerWalletAddress);
        return sendTransaction(credentials, functionData, didRegistryAddress);
    }



    /**
     * Check if a wallet has default admin role
     */
    public boolean hasDefaultAdminRole(String walletAddress) {
        try {
            // TODO: Criar método específico no ContractService para DEFAULT_ADMIN_ROLE
            // O método atual getHasRoleFunctionData usa ISSUER_ROLE_HASH constante
            // Temporariamente retornando false até implementarmos o método correto
            return false;

            /*
            String functionData = contractService.getHasRoleFunctionData(
                "0x0000000000000000000000000000000000000000000000000000000000000000", // DEFAULT_ADMIN_ROLE
                walletAddress
            );

            EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, didRegistryAddress, functionData),
                DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                System.out.println("Error checking admin role: " + response.getError().getMessage());
                return false;
            }
            */

        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar default admin role: " + e.getMessage(), e);
        }
    }

    /**
     * Send a transaction to the blockchain
     */
    private CompletableFuture<TransactionReceipt> sendTransaction(
            Credentials credentials, String functionData, String contractAddress) throws Exception {

        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();

        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice,
                gasLimit,
                contractAddress,
                functionData
        );

        byte[] signedMessage = org.web3j.crypto.TransactionEncoder.signMessage(rawTransaction, chainId, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        org.web3j.protocol.core.methods.response.EthSendTransaction ethSendTransaction =
                web3j.ethSendRawTransaction(hexValue).send();

        if (ethSendTransaction.hasError()) {
            throw new RuntimeException("Transaction failed: " + ethSendTransaction.getError().getMessage());
        }

        String txHash = ethSendTransaction.getTransactionHash();
        System.out.println("Transaction sent with hash: " + txHash);

        return waitForTransactionReceipt(txHash, 30, 2000);
    }

    /**
     * Grant issuer role to a wallet
     */
    public CompletableFuture<TransactionReceipt> grantIssuerRole(String targetWalletAddress) throws Exception {
        try {
            Credentials adminCredentials = walletService.getAdminCredentials();

            // Usar diretamente o método com ISSUER_ROLE_HASH constante
            String functionData = contractService.getGrantRoleFunctionData(targetWalletAddress);

            return sendTransaction(adminCredentials, functionData, didRegistryAddress);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao conceder ISSUER_ROLE: " + e.getMessage(), e);
        }
    }

    /**
     * Grant issuer role for a specific contract
     */
    private CompletableFuture<TransactionReceipt> grantIssuerRoleForContract(String targetWalletAddress, String contractAddress) throws Exception {
        try {
            Credentials adminCredentials = walletService.getAdminCredentials();

            // Usar diretamente o método com ISSUER_ROLE_HASH constante
            String functionData = contractService.getGrantRoleFunctionData(targetWalletAddress);

            return sendTransaction(adminCredentials, functionData, contractAddress);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao conceder ISSUER_ROLE para contrato " + contractAddress + ": " + e.getMessage(), e);
        }
    }

    /**
     * Grant issuer role with specific admin wallet
     */
    public CompletableFuture<TransactionReceipt> grantIssuerRole(String targetWalletAddress, String adminWalletAddress) throws Exception {
        return grantIssuerRoleForContract(targetWalletAddress, didRegistryAddress);
    }

    /**
     * Wait for transaction receipt
     */
    private CompletableFuture<TransactionReceipt> waitForTransactionReceipt(String txHash, int maxAttempts, long intervalMs) {
        return CompletableFuture.supplyAsync(() -> {
            for (int i = 0; i < maxAttempts; i++) {
                try {
                    Thread.sleep(intervalMs);

                    Optional<TransactionReceipt> receipt = web3j.ethGetTransactionReceipt(txHash)
                            .send()
                            .getTransactionReceipt();

                    if (receipt.isPresent()) {
                        TransactionReceipt txReceipt = receipt.get();
                        System.out.println("Transaction confirmed: " + txHash);
                        System.out.println("Gas used: " + txReceipt.getGasUsed());
                        System.out.println("Status: " + txReceipt.getStatus());
                        return txReceipt;
                    }

                } catch (Exception e) {
                    System.out.println("Error waiting for receipt (attempt " + (i + 1) + "): " + e.getMessage());
                }
            }

            throw new RuntimeException("Transaction receipt not received after " + maxAttempts + " attempts");
        });
    }

    /**
     * Get status list manager address
     */
    // Remover este método completamente:
    // public String getStatusListManagerAddress() {
    //     return statusListManagerAddress;
    // }

    /**
     * Get DID registry address
     */
    public String getDidRegistryAddress() {
        return didRegistryAddress;
    }

    /**
     * Revoke an attribute from the DID registry
     */
    public CompletableFuture<TransactionReceipt> revokeAttribute(
            String issuerWalletAddress,
            String credentialId,
            String metadataHash) throws Exception {

        String functionData = contractService.revokeAttributeFunctionData(
                credentialId, "credentialMetadata", metadataHash
        );


        Credentials credentials = walletService.getAdminCredentials();
        return sendTransaction(credentials, functionData, didRegistryAddress);
    }

    // ===== DID MANAGEMENT FUNCTIONS =====

    /**
     * Create a new DID with owner and document
     */
    public CompletableFuture<TransactionReceipt> createDID(
            String identity,
            String didDocument) throws Exception {

        String functionData = contractService.createDIDFunctionData(identity, didDocument);
        Credentials credentials = walletService.getAdminCredentials();
        return sendTransaction(credentials, functionData, didRegistryAddress);
    }

    /**
     * Update DID document
     */
    public CompletableFuture<TransactionReceipt> updateDIDDocument(
            String identity,
            String newDidDocument) throws Exception {

        String functionData = contractService.updateDIDDocumentFunctionData(identity, newDidDocument);
        Credentials credentials = walletService.getAdminCredentials();
        return sendTransaction(credentials, functionData, didRegistryAddress);
    }

    /**
     * Set KYC status for an identity
     */
    public CompletableFuture<TransactionReceipt> setKYCStatus(
            String identity,
            boolean verified) throws Exception {

        String functionData = contractService.setKYCStatusFunctionData(identity, verified);
        Credentials credentials = walletService.getAdminCredentials();
        return sendTransaction(credentials, functionData, didRegistryAddress);
    }

    /**
     * Check if DID exists
     */
    public boolean didExists(String identity) {
        try {
            String functionData = contractService.didExistsFunctionData(identity);

            EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, didRegistryAddress, functionData),
                DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                System.out.println("Error checking DID existence: " + response.getError().getMessage());
                return false;
            }

            String result = response.getValue();
            return !"0x0000000000000000000000000000000000000000000000000000000000000000".equals(result) &&
                   result.endsWith("1");

        } catch (Exception e) {
            System.out.println("Error checking DID existence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get identity information
     */
    public Optional<IdentityInfo> getIdentityInfo(String identity) {
        try {
            String functionData = contractService.getIdentityInfoFunctionData(identity);

            EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, didRegistryAddress, functionData),
                DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                System.out.println("Error getting identity info: " + response.getError().getMessage());
                return Optional.empty();
            }

            // Decode response and create IdentityInfo object
            String result = response.getValue();
            // TODO: Implement proper decoding based on contract structure

            return Optional.of(new IdentityInfo());

        } catch (Exception e) {
            System.out.println("Error getting identity info: " + e.getMessage());
            return Optional.empty();
        }
    }

    // ===== CREDENTIAL MANAGEMENT FUNCTIONS =====

    /**
     * Issue a verifiable credential
     */
    public CompletableFuture<TransactionReceipt> issueCredential(
            String credentialId,
            String subject,
            String credentialHash) throws Exception {

        String functionData = contractService.issueCredentialFunctionData(
                credentialId, subject, credentialHash
        );

        Credentials credentials = walletService.getAdminCredentials();
        return sendTransaction(credentials, functionData, didRegistryAddress);
    }

    /**
     * Revoke a verifiable credential
     */
    public CompletableFuture<TransactionReceipt> revokeCredential(
            String issuerWalletAddress,
            String credentialId,
            String subject,
            String reason) throws Exception {

        String functionData = contractService.revokeCredentialFunctionData(
                credentialId, subject, reason
        );

        Credentials credentials = walletService.getWalletCredentialsForBlockchain(issuerWalletAddress);
        return sendTransaction(credentials, functionData, didRegistryAddress);
    }

    /**
     * Restore a verifiable credential
     */
    public CompletableFuture<TransactionReceipt> restoreCredential(
            String credentialId,
            String subject,
            String reason) throws Exception {

        String functionData = contractService.restoreCredentialFunctionData(
                credentialId, subject, reason
        );

        Credentials credentials = walletService.getAdminCredentials();
        return sendTransaction(credentials, functionData, didRegistryAddress);
    }

    // ===== DELEGATE MANAGEMENT FUNCTIONS =====

    /**
     * Add a delegate to an identity
     */
    public CompletableFuture<TransactionReceipt> addDelegate(
            String identity,
            String delegateType,
            String delegate,
            Long validity) throws Exception {

        String functionData = contractService.addDelegateFunctionData(
                identity, delegateType, delegate, validity
        );

        Credentials credentials = walletService.getAdminCredentials();
        return sendTransaction(credentials, functionData, didRegistryAddress);
    }

    /**
     * Revoke a delegate from an identity
     */
    public CompletableFuture<TransactionReceipt> revokeDelegate(
            String identity,
            String delegateType,
            String delegate) throws Exception {

        String functionData = contractService.revokeDelegateFunctionData(
                identity, delegateType, delegate
        );

        Credentials credentials = walletService.getAdminCredentials();
        return sendTransaction(credentials, functionData, didRegistryAddress);
    }

    /**
     * Check if delegate is valid
     */
    public boolean isValidDelegate(String identity, String delegateType, String delegate) {
        try {
            String functionData = contractService.isValidDelegateFunctionData(
                    identity, delegateType, delegate
            );

            EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, didRegistryAddress, functionData),
                DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                System.out.println("Error checking delegate validity: " + response.getError().getMessage());
                return false;
            }

            String result = response.getValue();
            return !"0x0000000000000000000000000000000000000000000000000000000000000000".equals(result) &&
                   result.endsWith("1");

        } catch (Exception e) {
            System.out.println("Error checking delegate validity: " + e.getMessage());
            return false;
        }
    }

    // ===== SYSTEM METRICS FUNCTIONS =====

    /**
     * Check if credential is revoked
     */
    public boolean isCredentialRevoked(String credentialId) {
        try {
            String functionData = contractService.isCredentialRevokedFunctionData(credentialId);

            EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, didRegistryAddress, functionData),
                DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                System.out.println("Error checking credential revocation: " + response.getError().getMessage());
                return false;
            }

            String result = response.getValue();
            return !"0x0000000000000000000000000000000000000000000000000000000000000000".equals(result) &&
                   result.endsWith("1");

        } catch (Exception e) {
            System.out.println("Error checking credential revocation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get credential revocation record
     */
    public Optional<RevocationRecord> getCredentialRevocation(String credentialId) {
        try {
            String functionData = contractService.getCredentialRevocationFunctionData(credentialId);

            EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, didRegistryAddress, functionData),
                DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                System.out.println("Error getting credential revocation: " + response.getError().getMessage());
                return Optional.empty();
            }

            String result = response.getValue();
            // Check if result is empty (all zeros)
            if ("0x0000000000000000000000000000000000000000000000000000000000000000".equals(result) ||
                result.length() <= 66) {
                return Optional.empty();
            }

            // Decode the tuple response
            try {
                // Remove 0x prefix
                String hexData = result.substring(2);

                // Parse the tuple structure
                // First 32 bytes (64 hex chars) - offset to tuple data
                // Next 32 bytes - bool revoked (1 byte, padded to 32)
                // Next 32 bytes - uint256 timestamp
                // Next 32 bytes - address revoker (20 bytes, padded to 32)
                // Next 32 bytes - offset to string reason
                // Next 32 bytes - bytes32 credentialHash
                // Then the actual string data

                // Skip the first offset (64 chars)
                String dataSection = hexData.substring(64);

                // Parse revoked (bool) - next 64 chars, take last 2
                boolean revoked = !dataSection.substring(62, 64).equals("00");

                // Parse timestamp (uint256) - next 64 chars
                String timestampHex = dataSection.substring(64, 128);
                BigInteger timestamp = new BigInteger(timestampHex, 16);

                // Parse revoker address - next 64 chars, take last 40
                String revokerHex = dataSection.substring(128 + 24, 192);
                String revoker = "0x" + revokerHex;

                // Parse reason string offset - next 64 chars
                String reasonOffsetHex = dataSection.substring(192, 256);
                int reasonOffset = new BigInteger(reasonOffsetHex, 16).intValue();

                // Parse credentialHash - next 64 chars
                String credentialHashHex = dataSection.substring(256, 320);

                // Parse reason string
                String reason = "";
                if (reasonOffset > 0) {
                    // Reason starts at offset * 2 (hex chars)
                    int reasonStart = reasonOffset * 2;
                    if (reasonStart < hexData.length()) {
                        // First 32 bytes at reason location is the length
                        String reasonLengthHex = hexData.substring(reasonStart, reasonStart + 64);
                        int reasonLength = new BigInteger(reasonLengthHex, 16).intValue();

                        if (reasonLength > 0 && reasonStart + 64 + (reasonLength * 2) <= hexData.length()) {
                            String reasonHex = hexData.substring(reasonStart + 64, reasonStart + 64 + (reasonLength * 2));
                            reason = new String(Numeric.hexStringToByteArray(reasonHex));
                        }
                    }
                }

                // Create RevocationRecord with decoded data
                RevocationRecord record = new RevocationRecord();
                record.setCredentialId(credentialId);
                record.setRevoked(revoked);
                record.setRevoker(revoker);
                record.setRevokedAt(timestamp);
                record.setReason(reason);

                return Optional.of(record);

            } catch (Exception decodeError) {
                System.out.println("Error decoding revocation data: " + decodeError.getMessage());
                decodeError.printStackTrace();
                return Optional.empty();
            }

        } catch (Exception e) {
            System.out.println("Error getting credential revocation: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get KYC status for an identity
     */
    public boolean getKYCStatus(String identity) {
        try {
            String functionData = contractService.getKYCStatusFunctionData(identity);

            EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, didRegistryAddress, functionData),
                DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                System.out.println("Error getting KYC status: " + response.getError().getMessage());
                return false;
            }

            String result = response.getValue();
            return !"0x0000000000000000000000000000000000000000000000000000000000000000".equals(result) &&
                   result.endsWith("1");

        } catch (Exception e) {
            System.out.println("Error getting KYC status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Pause the contract (admin function)
     */
    public CompletableFuture<TransactionReceipt> pause() throws Exception {
        String functionData = contractService.pauseFunctionData();
        Credentials credentials = walletService.getAdminCredentials();
        return sendTransaction(credentials, functionData, didRegistryAddress);
    }

    /**
     * Unpause the contract (admin function)
     */
    public CompletableFuture<TransactionReceipt> unpause() throws Exception {
        String functionData = contractService.unpauseFunctionData();
        Credentials credentials = walletService.getAdminCredentials();
        return sendTransaction(credentials, functionData, didRegistryAddress);
    }

    /**
     * Get system metrics
     */
    public SystemMetrics getSystemMetrics() {
        try {
            String functionData = contractService.getSystemMetricsFunctionData();

            EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, didRegistryAddress, functionData),
                DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                throw new RuntimeException("Erro ao obter métricas do sistema: " + response.getError().getMessage());
            }

            // Decode response and create SystemMetrics object
            String result = response.getValue();
            // TODO: Implement proper decoding based on contract structure

            return new SystemMetrics();

        } catch (Exception e) {
            System.out.println("Error getting system metrics: " + e.getMessage());
            return new SystemMetrics(); // Return empty metrics on error
        }
    }

    /**
     * Check if a wallet has issuer role
     */
    public boolean hasIssuerRole(String walletAddress) {
        try {
            String functionData = contractService.getHasRoleFunctionData(walletAddress);

            EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, didRegistryAddress, functionData),
                DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                throw new RuntimeException("Erro ao verificar hasRole: " + response.getError().getMessage());
            }

            String result = response.getValue();
            return !result.equals("0x0000000000000000000000000000000000000000000000000000000000000000");

        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar role de issuer: " + e.getMessage(), e);
        }
    }

    /**
     * Check if a wallet has issuer role for a specific contract
     */
    public boolean hasIssuerRoleForContract(String contractAddress, String walletAddress) {
        try {
            String hasRoleFunctionData = contractService.getHasRoleFunctionData(walletAddress);

            org.web3j.protocol.core.methods.request.Transaction hasRoleTransaction =
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                    null, contractAddress, hasRoleFunctionData
                );

            org.web3j.protocol.core.methods.response.EthCall hasRoleCall = web3j.ethCall(hasRoleTransaction,
                org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send();

            if (hasRoleCall.hasError()) {
                throw new RuntimeException("Erro ao verificar hasRole no contrato " + contractAddress + ": " + hasRoleCall.getError().getMessage());
            }

            String result = hasRoleCall.getValue();
            return !result.equals("0x0000000000000000000000000000000000000000000000000000000000000000");

        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar role de issuer para contrato " + contractAddress + ": " + e.getMessage(), e);
        }
    }

    /**
     * Check if credential exists in the contract
     */
    public boolean credentialExists(String credentialId) {
        try {
            // Use getCredentialRevocation to check if credential exists
            // If it returns valid data (not all zeros), credential exists
            String functionData = contractService.getCredentialRevocationFunctionData(credentialId);

            EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(null, didRegistryAddress, functionData),
                DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                System.out.println("Error checking credential existence: " + response.getError().getMessage());
                return false;
            }

            String result = response.getValue();
            // If result is not all zeros, credential exists (even if not revoked)
            return !"0x0000000000000000000000000000000000000000000000000000000000000000".equals(result) &&
                   result.length() > 66; // Valid response should be longer than empty bytes32

        } catch (Exception e) {
            System.out.println("Error checking credential existence: " + e.getMessage());
            return false;
        }
    }
}

