package br.com.idhub.custody.web;

import br.com.idhub.custody.domain.WalletMetadata;
import br.com.idhub.custody.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/wallets")
@CrossOrigin(origins = "*")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public ResponseEntity<WalletMetadata> createWallet(
            @RequestParam(name = "name") String name,
            @RequestParam(name = "description") String description) {
        try {
            WalletMetadata wallet = walletService.createWallet(name, description);
            return ResponseEntity.ok(wallet);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/with-password")
    public ResponseEntity<WalletMetadata> createWalletWithPassword(
            @RequestParam(name = "name") String name,
            @RequestParam(name = "description") String description,
            @RequestParam(name = "password") String password) {
        try {
            WalletMetadata wallet = walletService.createWalletWithPassword(name, description, password);
            return ResponseEntity.ok(wallet);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<WalletMetadata>> getAllWallets() {
        try {
            List<WalletMetadata> wallets = walletService.getAllWallets();
            return ResponseEntity.ok(wallets);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{address}")
    public ResponseEntity<WalletMetadata> getWalletInfo(@PathVariable String address) {
        try {
            WalletMetadata wallet = walletService.getWalletInfo(address);
            return ResponseEntity.ok(wallet);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{address}")
    public ResponseEntity<WalletMetadata> updateWallet(
            @PathVariable String address,
            @RequestParam(name = "name") String name,
            @RequestParam(name = "description") String description) {
        try {
            WalletMetadata wallet = walletService.updateWallet(address, name, description);
            return ResponseEntity.ok(wallet);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{address}")
    public ResponseEntity<Boolean> deactivateWallet(@PathVariable String address) {
        try {
            boolean result = walletService.deactivateWallet(address);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{address}/balance")
    public ResponseEntity<String> getBalance(@PathVariable String address) {
        try {
            String balance = walletService.getFormattedBalance(address);
            return ResponseEntity.ok(balance);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao obter saldo: " + e.getMessage());
        }
    }

    @GetMapping("/{address}/balance/raw")
    public ResponseEntity<BigInteger> getRawBalance(@PathVariable String address) {
        try {
            CompletableFuture<BigInteger> balanceFuture = walletService.getBalance(address);
            BigInteger balance = balanceFuture.get();
            return ResponseEntity.ok(balance);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/credentials")
    public ResponseEntity<String> getCredentials() {
        try {
            String address = walletService.getCredentials().getAddress();
            return ResponseEntity.ok(address);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{address}/credentials")
    public ResponseEntity<String> getWalletCredentials(
            @PathVariable String address,
            @RequestParam(name = "password") String password) {
        try {
            String walletAddress = walletService.getWalletCredentials(address, password).getAddress();
            return ResponseEntity.ok(walletAddress);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao obter credenciais: " + e.getMessage());
        }
    }

    @GetMapping("/{address}/credentials/master")
    public ResponseEntity<String> getWalletCredentialsWithMasterPassword(@PathVariable String address) {
        try {
            String walletAddress = walletService.getWalletCredentialsWithMasterPassword(address).getAddress();
            return ResponseEntity.ok(walletAddress);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao obter credenciais: " + e.getMessage());
        }
    }

    // ⚠️ APENAS PARA DESENVOLVIMENTO - Obter chave privada descriptografada
    @GetMapping("/{address}/private-key/dev")
    public ResponseEntity<String> getPrivateKeyForDevelopment(@PathVariable String address) {
        try {
            String privateKey = walletService.getPrivateKeyForDevelopment(address);
            return ResponseEntity.ok(privateKey);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao obter chave privada: " + e.getMessage());
        }
    }

    // ⚠️ APENAS PARA DESENVOLVIMENTO - Obter chave privada criptografada
    @GetMapping("/{address}/private-key/dev/encrypted")
    public ResponseEntity<String> getEncryptedPrivateKeyForDevelopment(@PathVariable String address) {
        try {
            String encryptedPrivateKey = walletService.getEncryptedPrivateKeyForDevelopment(address);
            return ResponseEntity.ok(encryptedPrivateKey);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao obter chave privada criptografada: " + e.getMessage());
        }
    }

    // Importar wallet administrativa a partir da variável de ambiente
    @PostMapping("/import-admin")
    public ResponseEntity<?> importAdminWallet() {
        try {
            WalletMetadata wallet = walletService.importAdminWalletFromEnv(
                "Admin Wallet",
                "Wallet administrativa para operações de smart contract"
            );
            return ResponseEntity.ok(wallet);
        } catch (Exception e) {
            // Retornar mensagem de erro detalhada para debug
            return ResponseEntity.badRequest().body("Erro ao importar wallet administrativa: " + e.getMessage());
        }
    }

}
