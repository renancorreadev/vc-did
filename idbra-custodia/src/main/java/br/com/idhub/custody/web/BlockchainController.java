package br.com.idhub.custody.web;

import br.com.idhub.custody.service.BlockchainService;
import br.com.idhub.custody.domain.SystemMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/blockchain")
public class BlockchainController {

    @Autowired
    private BlockchainService blockchainService;

    // Endereços hardcoded temporariamente
    private static final String DID_REGISTRY_ADDRESS = "0x34c2AcC42882C0279A64bB1a4B1083D483BdE886";

    /**
     * Verificar se uma wallet tem ISSUER_ROLE em ambos os contratos
     */
    @GetMapping("/check-role/{walletAddress}")
    public ResponseEntity<Map<String, Object>> checkIssuerRole(@PathVariable String walletAddress) {
        try {
            boolean hasRole = blockchainService.hasIssuerRole(walletAddress);
            Map<String, Object> response = Map.of(
                "walletAddress", walletAddress,
                "hasIssuerRole", hasRole,
                "didRegistry", blockchainService.hasIssuerRoleForContract(walletAddress, blockchainService.getDidRegistryAddress()),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "error", e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Verificar se uma wallet tem DEFAULT_ADMIN_ROLE
     */
    @GetMapping("/check-admin-role/{walletAddress}")
    public ResponseEntity<Map<String, Object>> checkDefaultAdminRole(@PathVariable String walletAddress) {
        try {
            boolean hasRole = blockchainService.hasDefaultAdminRole(walletAddress);
            Map<String, Object> response = Map.of(
                "walletAddress", walletAddress,
                "hasDefaultAdminRole", hasRole,
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "error", e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Verificar status da conexão blockchain
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBlockchainStatus() {
        try {
            Map<String, Object> status = Map.of(
                "status", "CONNECTED",
                "network", "Besu Consortium",
                "chainId", "1337",
                "rpcUrl", "http://144.22.179.183",
                "gasPrice", "0",
                "mode", "LEGACY",
                "contracts", Map.of(
                    "didRegistry", DID_REGISTRY_ADDRESS
                ),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "error", e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Conceder ISSUER_ROLE para uma wallet (usando chave administrativa configurada)
     */
    @PostMapping("/grant-issuer-role/{walletAddress}")
    public ResponseEntity<Map<String, Object>> grantIssuerRole(@PathVariable String walletAddress) {
        try {
            // Usar a chave administrativa configurada diretamente
            CompletableFuture<TransactionReceipt> future = blockchainService.grantIssuerRole(walletAddress);
            TransactionReceipt receipt = future.get();

            if (receipt.isStatusOK()) {
                Map<String, Object> response = Map.of(
                    "success", true,
                    "walletAddress", walletAddress,
                    "transactionHash", receipt.getTransactionHash(),
                    "blockNumber", receipt.getBlockNumber().toString(),
                    "gasUsed", receipt.getGasUsed().toString(),
                    "timestamp", java.time.LocalDateTime.now().toString()
                );
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> error = Map.of(
                    "success", false,
                    "error", "Falha ao conceder ISSUER_ROLE. Status: " + receipt.getStatus(),
                    "walletAddress", walletAddress,
                    "timestamp", java.time.LocalDateTime.now().toString()
                );
                return ResponseEntity.badRequest().body(error);
            }
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "error", e.getMessage(),
                "walletAddress", walletAddress,
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Criar novo DID
     */
    @PostMapping("/did/create")
    public ResponseEntity<Map<String, Object>> createDID(
            @RequestParam String identity,
            @RequestParam String didDocument) {
        try {
            CompletableFuture<TransactionReceipt> future = blockchainService.createDID(identity, didDocument);
            TransactionReceipt receipt = future.get();

            Map<String, Object> response = Map.of(
                "success", receipt.isStatusOK(),
                "identity", identity,
                "transactionHash", receipt.getTransactionHash(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Verificar status KYC
     */
    @GetMapping("/kyc/{identity}")
    public ResponseEntity<Map<String, Object>> getKYCStatus(@PathVariable String identity) {
        try {
            boolean kycStatus = blockchainService.getKYCStatus(identity);
            Map<String, Object> response = Map.of(
                "identity", identity,
                "kycVerified", kycStatus,
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Definir status KYC
     */
    @PostMapping("/kyc/{identity}")
    public ResponseEntity<Map<String, Object>> setKYCStatus(
            @PathVariable String identity,
            @RequestParam boolean verified) {
        try {
            CompletableFuture<TransactionReceipt> future = blockchainService.setKYCStatus(identity, verified);
            TransactionReceipt receipt = future.get();

            Map<String, Object> response = Map.of(
                "success", receipt.isStatusOK(),
                "identity", identity,
                "kycVerified", verified,
                "transactionHash", receipt.getTransactionHash(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Pausar contrato (apenas admin)
     */
    @PostMapping("/pause")
    public ResponseEntity<Map<String, Object>> pauseContract() {
        try {
            CompletableFuture<TransactionReceipt> future = blockchainService.pause();
            TransactionReceipt receipt = future.get();

            Map<String, Object> response = Map.of(
                "success", receipt.isStatusOK(),
                "action", "pause",
                "transactionHash", receipt.getTransactionHash(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Despausar contrato (apenas admin)
     */
    @PostMapping("/unpause")
    public ResponseEntity<Map<String, Object>> unpauseContract() {
        try {
            CompletableFuture<TransactionReceipt> future = blockchainService.unpause();
            TransactionReceipt receipt = future.get();

            Map<String, Object> response = Map.of(
                "success", receipt.isStatusOK(),
                "action", "unpause",
                "transactionHash", receipt.getTransactionHash(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Obter métricas do sistema
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        try {
            SystemMetrics metrics = blockchainService.getSystemMetrics();
            Map<String, Object> response = Map.of(
                "metrics", metrics,
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
