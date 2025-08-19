package br.com.idhub.custody.web;

import br.com.idhub.custody.domain.StatusList;
import br.com.idhub.custody.service.StatusListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/statuslist")
public class StatusListController {

    @Autowired
    private StatusListService statusListService;

    /**
     * Criar nova StatusList
     */
    @PostMapping
    public ResponseEntity<StatusList> createStatusList(
            @RequestParam String listId,
            @RequestParam String uri,
            @RequestParam String purpose,
            @RequestParam String issuer,
            @RequestParam String issuerWalletAddress) {
        try {
            StatusList result = statusListService.createStatusList(
                listId, uri, purpose, issuer, issuerWalletAddress);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Atualizar StatusList existente
     */
    @PutMapping("/{listId}")
    public ResponseEntity<StatusList> updateStatusList(
            @PathVariable String listId,
            @RequestParam String newUri,
            @RequestParam String issuerWalletAddress) {
        try {
            StatusList result = statusListService.updateStatusList(listId, newUri, issuerWalletAddress);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Revogar credencial na StatusList
     */
    @PostMapping("/{listId}/revoke/{index}")
    public ResponseEntity<Boolean> revokeCredential(
            @PathVariable String listId,
            @PathVariable Integer index) {
        try {
            boolean result = statusListService.revokeCredentialInList(listId, index);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(false);
        }
    }

    /**
     * Verificar status de uma credencial
     */
    @GetMapping("/{listId}/status/{index}")
    public ResponseEntity<Boolean> checkCredentialStatus(
            @PathVariable String listId,
            @PathVariable Integer index) {
        try {
            boolean isRevoked = statusListService.isCredentialRevoked(listId, index);
            return ResponseEntity.ok(isRevoked);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obter StatusList como JSON
     */
    @GetMapping("/{listId}/json")
    public ResponseEntity<String> getStatusListJson(@PathVariable String listId) {
        try {
            String json = statusListService.getStatusListAsJson(listId);
            return ResponseEntity.ok(json);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obter metadados da StatusList
     */
    @GetMapping("/{listId}/metadata")
    public ResponseEntity<Map<String, Object>> getStatusListMetadata(@PathVariable String listId) {
        try {
            Map<String, Object> metadata = statusListService.getStatusListMetadata(listId);
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Listar todas as StatusLists
     */
    @GetMapping
    public ResponseEntity<List<StatusList>> getAllStatusLists() {
        try {
            List<StatusList> statusLists = statusListService.getAllStatusLists();
            return ResponseEntity.ok(statusLists);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Obter StatusList por ID
     */
    @GetMapping("/{listId}")
    public ResponseEntity<StatusList> getStatusList(@PathVariable String listId) {
        try {
            Optional<StatusList> statusList = statusListService.getStatusListById(listId);
            if (statusList.isPresent()) {
                return ResponseEntity.ok(statusList.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
