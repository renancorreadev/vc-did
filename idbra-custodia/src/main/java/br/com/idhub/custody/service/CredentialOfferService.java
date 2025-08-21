package br.com.idhub.custody.service;

import br.com.idhub.custody.domain.CredentialOffer;
import br.com.idhub.custody.repository.CredentialOfferRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CredentialOfferService {

    @Autowired
    private CredentialOfferRepository credentialOfferRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Criar nova oferta de credencial
     */
    public CredentialOffer createOffer(String holderName, String holderEmail, String holderCpf,
                                      String credentialType, Object requestedData, String issuerDid) throws Exception {

        // Verificar se já existe oferta pendente para este CPF e tipo de credencial
        boolean existsPending = credentialOfferRepository.existsByHolderCpfAndCredentialTypeAndStatus(
            holderCpf, credentialType, CredentialOffer.OfferStatus.PENDING);

        if (existsPending) {
            throw new IllegalStateException("Já existe uma oferta pendente para este CPF e tipo de credencial");
        }

        // Gerar ID único para a oferta
        String offerId = generateOfferId();

        // Converter dados solicitados para JSON
        String requestedDataJson = objectMapper.writeValueAsString(requestedData);

        // Criar oferta
        CredentialOffer offer = new CredentialOffer(
            offerId, holderName, holderEmail, holderCpf,
            credentialType, requestedDataJson, issuerDid
        );

        return credentialOfferRepository.save(offer);
    }

    /**
     * Aprovar oferta
     */
    public CredentialOffer approveOffer(String offerId, String approverWalletAddress) throws Exception {
        Optional<CredentialOffer> offerOpt = credentialOfferRepository.findByOfferId(offerId);

        if (offerOpt.isEmpty()) {
            throw new IllegalArgumentException("Oferta não encontrada: " + offerId);
        }

        CredentialOffer offer = offerOpt.get();

        if (offer.getStatus() != CredentialOffer.OfferStatus.PENDING) {
            throw new IllegalStateException("Oferta não está pendente: " + offer.getStatus());
        }

        offer.setStatus(CredentialOffer.OfferStatus.APPROVED);
        offer.setApprovedAt(LocalDateTime.now());
        offer.setApprovedBy(approverWalletAddress);

        return credentialOfferRepository.save(offer);
    }

    /**
     * Rejeitar oferta
     */
    public CredentialOffer rejectOffer(String offerId, String rejectionReason) throws Exception {
        Optional<CredentialOffer> offerOpt = credentialOfferRepository.findByOfferId(offerId);

        if (offerOpt.isEmpty()) {
            throw new IllegalArgumentException("Oferta não encontrada: " + offerId);
        }

        CredentialOffer offer = offerOpt.get();

        if (offer.getStatus() != CredentialOffer.OfferStatus.PENDING) {
            throw new IllegalStateException("Oferta não está pendente: " + offer.getStatus());
        }

        offer.setStatus(CredentialOffer.OfferStatus.REJECTED);
        offer.setRejectionReason(rejectionReason);

        return credentialOfferRepository.save(offer);
    }

    /**
     * Marcar oferta como completada (credencial emitida)
     */
    public CredentialOffer completeOffer(String offerId, String holderWalletAddress) throws Exception {
        Optional<CredentialOffer> offerOpt = credentialOfferRepository.findByOfferId(offerId);

        if (offerOpt.isEmpty()) {
            throw new IllegalArgumentException("Oferta não encontrada: " + offerId);
        }

        CredentialOffer offer = offerOpt.get();

        if (offer.getStatus() != CredentialOffer.OfferStatus.APPROVED) {
            throw new IllegalStateException("Oferta não está aprovada: " + offer.getStatus());
        }

        offer.setStatus(CredentialOffer.OfferStatus.COMPLETED);
        offer.setHolderWalletAddress(holderWalletAddress);

        return credentialOfferRepository.save(offer);
    }

    /**
     * Buscar oferta por ID
     */
    public Optional<CredentialOffer> findByOfferId(String offerId) {
        return credentialOfferRepository.findByOfferId(offerId);
    }

    /**
     * Buscar ofertas pendentes por emissor
     */
    public List<CredentialOffer> findPendingOffersByIssuer(String issuerDid) {
        return credentialOfferRepository.findPendingOffersByIssuer(issuerDid);
    }

    /**
     * Buscar ofertas aprovadas por CPF
     */
    public List<CredentialOffer> findApprovedOffersByCpf(String cpf) {
        return credentialOfferRepository.findApprovedOffersByCpf(cpf);
    }

    /**
     * Verificar se holder tem oferta aprovada
     */
    public boolean hasApprovedOffer(String holderCpf, String credentialType) {
        List<CredentialOffer> approvedOffers = findApprovedOffersByCpf(holderCpf);
        return approvedOffers.stream()
            .anyMatch(offer -> credentialType.equals(offer.getCredentialType()));
    }

    private String generateOfferId() {
        return "OFFER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
