package br.com.idhub.custody.repository;

import br.com.idhub.custody.domain.CredentialOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CredentialOfferRepository extends JpaRepository<CredentialOffer, Long> {

    Optional<CredentialOffer> findByOfferId(String offerId);

    List<CredentialOffer> findByStatus(CredentialOffer.OfferStatus status);

    List<CredentialOffer> findByIssuerDid(String issuerDid);

    List<CredentialOffer> findByHolderCpf(String holderCpf);

    List<CredentialOffer> findByCredentialType(String credentialType);

    Optional<CredentialOffer> findByHolderWalletAddress(String holderWalletAddress);

    @Query("SELECT co FROM CredentialOffer co WHERE co.status = 'PENDING' AND co.issuerDid = :issuerDid")
    List<CredentialOffer> findPendingOffersByIssuer(@Param("issuerDid") String issuerDid);

    @Query("SELECT co FROM CredentialOffer co WHERE co.holderCpf = :cpf AND co.status = 'APPROVED'")
    List<CredentialOffer> findApprovedOffersByCpf(@Param("cpf") String cpf);

    boolean existsByOfferId(String offerId);

    boolean existsByHolderCpfAndCredentialTypeAndStatus(String holderCpf, String credentialType, CredentialOffer.OfferStatus status);
}
