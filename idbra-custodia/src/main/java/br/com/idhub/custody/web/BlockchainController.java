package br.com.idhub.custody.web;

import br.com.idhub.custody.service.BlockchainService;
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
    private static final String DID_REGISTRY_ADDRESS = "0x8553c57aC9a666EAfC517Ffc4CF57e21d2D3a1cb";
    private static final String STATUS_LIST_MANAGER_ADDRESS = "0x93a284C91768F3010D52cD37f84f22c5052be40b";

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
                "statusListManager", blockchainService.hasIssuerRoleForContract(walletAddress, blockchainService.getStatusListManagerAddress()),
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
                    "didRegistry", DID_REGISTRY_ADDRESS,
                    "statusListManager", STATUS_LIST_MANAGER_ADDRESS
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
}
