package br.com.idhub.custody.web;

import br.com.idhub.custody.domain.WalletMetadata;
import br.com.idhub.custody.domain.CredentialOffer;
import br.com.idhub.custody.service.WalletService;
import br.com.idhub.custody.service.CredentialOfferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.Credentials;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/wallets")
@CrossOrigin(origins = "*")
public class WalletController {

    private final WalletService walletService;

    @Autowired
    private CredentialOfferService credentialOfferService;

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
            return ResponseEntity.badRequest().build();
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
            return ResponseEntity.badRequest().build();
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
            // Este método não existe no WalletService, vou implementar uma versão básica
            return ResponseEntity.ok("Credentials endpoint - implementação necessária");
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{address}/credentials")
    public ResponseEntity<String> getWalletCredentials(
            @PathVariable String address,
            @RequestParam(name = "password") String password) {
        try {
            Credentials credentials = walletService.getWalletCredentials(address, password);
            return ResponseEntity.ok(credentials.getAddress());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{address}/credentials/master")
    public ResponseEntity<String> getWalletCredentialsWithMasterPassword(@PathVariable String address) {
        try {
            Credentials credentials = walletService.getWalletCredentialsWithMasterPassword(address);
            return ResponseEntity.ok(credentials.getAddress());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{address}/private-key/dev")
    public ResponseEntity<String> getPrivateKeyForDevelopment(@PathVariable String address) {
        try {
            String privateKey = walletService.getPrivateKeyForDevelopment(address);
            return ResponseEntity.ok(privateKey);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{address}/private-key/dev/encrypted")
    public ResponseEntity<String> getEncryptedPrivateKeyForDevelopment(@PathVariable String address) {
        try {
            String encryptedPrivateKey = walletService.getEncryptedPrivateKeyForDevelopment(address);
            return ResponseEntity.ok(encryptedPrivateKey);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/import-admin")
    public ResponseEntity<?> importAdminWallet() {
        try {
            WalletMetadata result = walletService.importAdminWalletFromEnv(
                "Admin Wallet",
                "Wallet administrativa importada do ambiente"
            );
            return ResponseEntity.ok(Map.of(
                "message", "Admin wallet imported successfully",
                "wallet", result,
                "status", "completed"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to import admin wallet: " + e.getMessage()
            ));
        }
    }

    /**
     * Criar wallet do holder com verificação de oferta aprovada
     */
    @PostMapping("/holder")
    public ResponseEntity<Map<String, Object>> createHolderWallet(
            @RequestParam(name = "name") String name,
            @RequestParam(name = "description") String description,
            @RequestParam(name = "holderCpf") String holderCpf,
            @RequestParam(name = "credentialType") String credentialType,
            @RequestParam(name = "offerId") String offerId) {
        try {
            // 1. Verificar se existe oferta aprovada
            Optional<CredentialOffer> offerOpt = credentialOfferService.findByOfferId(offerId);

            if (offerOpt.isEmpty()) {
                Map<String, Object> error = Map.of(
                    "success", false,
                    "error", "Oferta não encontrada: " + offerId,
                    "timestamp", java.time.LocalDateTime.now().toString()
                );
                return ResponseEntity.badRequest().body(error);
            }

            CredentialOffer offer = offerOpt.get();

            // 2. Verificar se a oferta está aprovada
            if (offer.getStatus() != CredentialOffer.OfferStatus.APPROVED) {
                Map<String, Object> error = Map.of(
                    "success", false,
                    "error", "Oferta não está aprovada. Status atual: " + offer.getStatus(),
                    "timestamp", java.time.LocalDateTime.now().toString()
                );
                return ResponseEntity.badRequest().body(error);
            }

            // 3. Verificar se os dados conferem
            if (!holderCpf.equals(offer.getHolderCpf()) || !credentialType.equals(offer.getCredentialType())) {
                Map<String, Object> error = Map.of(
                    "success", false,
                    "error", "Dados não conferem com a oferta aprovada",
                    "timestamp", java.time.LocalDateTime.now().toString()
                );
                return ResponseEntity.badRequest().body(error);
            }

            // 4. Criar wallet do holder
            WalletMetadata wallet = walletService.createWallet(name, description);

            // 5. Marcar oferta como completada
            credentialOfferService.completeOffer(offerId, wallet.getAddress());

            Map<String, Object> response = Map.of(
                "success", true,
                "wallet", wallet,
                "offerId", offerId,
                "message", "Wallet do holder criada com sucesso após verificação de oferta aprovada",
                "timestamp", java.time.LocalDateTime.now().toString()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "error", "Erro ao criar wallet do holder: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.badRequest().body(error);
        }
    }
}
