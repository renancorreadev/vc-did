package br.com.idhub.custody.web;

import br.com.idhub.custody.domain.CredentialOffer;
import br.com.idhub.custody.service.CredentialOfferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/credential-offers")
@CrossOrigin(origins = "*")
public class CredentialOfferController {

    @Autowired
    private CredentialOfferService credentialOfferService;

    /**
     * Criar nova oferta de credencial (usado pelo holder/cliente)
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOffer(@RequestBody Map<String, Object> request) {
        try {
            String holderName = (String) request.get("holderName");
            String holderEmail = (String) request.get("holderEmail");
            String holderCpf = (String) request.get("holderCpf");
            String credentialType = (String) request.get("credentialType");
            Object requestedData = request.get("requestedData");
            String issuerDid = (String) request.get("issuerDid");

            CredentialOffer offer = credentialOfferService.createOffer(
                holderName, holderEmail, holderCpf, credentialType, requestedData, issuerDid
            );

            Map<String, Object> response = Map.of(
                "success", true,
                "offerId", offer.getOfferId(),
                "status", offer.getStatus().toString(),
                "message", "Oferta de credencial criada com sucesso",
                "timestamp", java.time.LocalDateTime.now().toString()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "error", "Erro ao criar oferta: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Aprovar oferta (usado pelo emissor)
     */
    @PostMapping("/{offerId}/approve")
    public ResponseEntity<Map<String, Object>> approveOffer(
            @PathVariable String offerId,
            @RequestParam String approverWalletAddress) {
        try {
            CredentialOffer offer = credentialOfferService.approveOffer(offerId, approverWalletAddress);

            Map<String, Object> response = Map.of(
                "success", true,
                "offerId", offer.getOfferId(),
                "status", offer.getStatus().toString(),
                "approvedAt", offer.getApprovedAt().toString(),
                "message", "Oferta aprovada com sucesso",
                "timestamp", java.time.LocalDateTime.now().toString()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "error", "Erro ao aprovar oferta: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Rejeitar oferta (usado pelo emissor)
     */
    @PostMapping("/{offerId}/reject")
    public ResponseEntity<Map<String, Object>> rejectOffer(
            @PathVariable String offerId,
            @RequestParam String rejectionReason) {
        try {
            CredentialOffer offer = credentialOfferService.rejectOffer(offerId, rejectionReason);

            Map<String, Object> response = Map.of(
                "success", true,
                "offerId", offer.getOfferId(),
                "status", offer.getStatus().toString(),
                "rejectionReason", offer.getRejectionReason(),
                "message", "Oferta rejeitada",
                "timestamp", java.time.LocalDateTime.now().toString()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = Map.of(
                "success", false,
                "error", "Erro ao rejeitar oferta: " + e.getMessage(),
                "timestamp", java.time.LocalDateTime.now().toString()
            );
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Buscar oferta por ID
     */
    @GetMapping("/{offerId}")
    public ResponseEntity<CredentialOffer> getOffer(@PathVariable String offerId) {
        Optional<CredentialOffer> offer = credentialOfferService.findByOfferId(offerId);
        return offer.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Listar ofertas pendentes por emissor
     */
    @GetMapping("/pending")
    public ResponseEntity<List<CredentialOffer>> getPendingOffers(@RequestParam String issuerDid) {
        List<CredentialOffer> offers = credentialOfferService.findPendingOffersByIssuer(issuerDid);
        return ResponseEntity.ok(offers);
    }

    /**
     * Verificar se holder tem oferta aprovada
     */
    @GetMapping("/check-approval")
    public ResponseEntity<Map<String, Object>> checkApproval(
            @RequestParam String holderCpf,
            @RequestParam String credentialType) {
        boolean hasApproved = credentialOfferService.hasApprovedOffer(holderCpf, credentialType);

        Map<String, Object> response = Map.of(
            "hasApprovedOffer", hasApproved,
            "holderCpf", holderCpf,
            "credentialType", credentialType,
            "timestamp", java.time.LocalDateTime.now().toString()
        );

        return ResponseEntity.ok(response);
    }
}
