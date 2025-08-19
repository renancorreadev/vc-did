package br.com.idhub.custody.web;

import br.com.idhub.custody.domain.*;
import br.com.idhub.custody.service.CredentialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/credentials")
public class CredentialController {

    @Autowired
    private CredentialService credentialService;

    /**
     * Criar credencial verificável
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createCredential(@RequestBody CredentialRequest request) {
        try {
            String vc = credentialService.createCredential(request);

            // Extrair o credentialId do JWT para retornar na resposta
            String credentialId = credentialService.extractCredentialIdFromJWT(vc);

            Map<String, Object> response = Map.of(
                "credentialId", credentialId,
                "jwt", vc,
                "success", true,
                "timestamp", java.time.LocalDateTime.now().toString()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "error", "Erro ao criar credencial: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Verificar credencial
     */
    @PostMapping("/verify")
    public ResponseEntity<CredentialVerification> verifyCredential(@RequestBody String credential) {
        try {
            CredentialVerification result = credentialService.verifyCredential(credential);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Revogar credencial
     */
    @PostMapping("/{credentialId}/revoke")
    public ResponseEntity<Map<String, Object>> revokeCredential(@PathVariable String credentialId) {
        try {
            System.out.println("\n=== INICIANDO REVOGAÇÃO DE CREDENCIAL ===");
            System.out.println("Credential ID: " + credentialId);

            boolean result = credentialService.revokeCredential(credentialId);

            System.out.println("Resultado da revogação: " + result);
            System.out.println("=== REVOGAÇÃO CONCLUÍDA COM SUCESSO ===");

            Map<String, Object> response = Map.of(
                "success", true,
                "revoked", result,
                "credentialId", credentialId,
                "message", "Credencial revogada com sucesso",
                "timestamp", java.time.LocalDateTime.now().toString()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("\n=== ERRO NA REVOGAÇÃO DE CREDENCIAL ===");
            System.err.println("Credential ID: " + credentialId);
            System.err.println("Erro: " + e.getClass().getSimpleName());
            System.err.println("Mensagem: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== FIM DO ERRO ===");

            Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", "Erro ao revogar credencial: " + e.getMessage(),
                "credentialId", credentialId,
                "timestamp", java.time.LocalDateTime.now().toString()
            );

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Criar ou atualizar StatusList
     */
    @PostMapping("/statuslist")
    public ResponseEntity<StatusList> createOrUpdateStatusList(
            @RequestParam String listId,
            @RequestParam String uri,
            @RequestParam String purpose,
            @RequestParam String issuer,
            @RequestParam String issuerWalletAddress) {
        try {
            StatusList result = credentialService.createOrUpdateStatusList(
                listId, uri, purpose, issuer, issuerWalletAddress);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Listar todas as credenciais
     */
    @GetMapping
    public ResponseEntity<List<Credential>> getAllCredentials() {
        try {
            List<Credential> credentials = credentialService.getAllCredentials();
            return ResponseEntity.ok(credentials);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obter credencial por ID
     */
    @GetMapping("/{credentialId}")
    public ResponseEntity<Credential> getCredential(@PathVariable String credentialId) {
        try {
            Optional<Credential> credential = credentialService.getCredentialById(credentialId);
            if (credential.isPresent()) {
                return ResponseEntity.ok(credential.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Listar credenciais por emissor
     */
    @GetMapping("/issuer/{issuerDid}")
    public ResponseEntity<List<Credential>> getCredentialsByIssuer(@PathVariable String issuerDid) {
        try {
            List<Credential> credentials = credentialService.getCredentialsByIssuer(issuerDid);
            return ResponseEntity.ok(credentials);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Listar credenciais por holder
     */
    @GetMapping("/holder/{holderDid}")
    public ResponseEntity<List<Credential>> getCredentialsByHolder(@PathVariable String holderDid) {
        try {
            List<Credential> credentials = credentialService.getCredentialsByHolder(holderDid);
            return ResponseEntity.ok(credentials);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Listar credenciais por status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Credential>> getCredentialsByStatus(@PathVariable String status) {
        try {
            List<Credential> credentials = credentialService.getCredentialsByStatus(status);
            return ResponseEntity.ok(credentials);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obter StatusList por ID
     */
    @GetMapping("/statuslist/{listId}")
    public ResponseEntity<StatusList> getStatusList(@PathVariable String listId) {
        try {
            Optional<StatusList> statusList = credentialService.getStatusListById(listId);
            if (statusList.isPresent()) {
                return ResponseEntity.ok(statusList.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Listar StatusLists por emissor
     */
    @GetMapping("/statuslist/issuer/{issuer}")
    public ResponseEntity<List<StatusList>> getStatusListsByIssuer(@PathVariable String issuer) {
        try {
            List<StatusList> statusLists = credentialService.getStatusListsByIssuer(issuer);
            return ResponseEntity.ok(statusLists);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
