package br.com.idhub.custody.repository;

import br.com.idhub.custody.domain.Credential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CredentialRepository extends JpaRepository<Credential, Long> {

    Optional<Credential> findByCredentialId(String credentialId);

    List<Credential> findByIssuerDid(String issuerDid);

    List<Credential> findByHolderDid(String holderDid);

    List<Credential> findByStatus(String status);

    List<Credential> findByStatusListId(String statusListId);

    Optional<Credential> findByStatusListIdAndStatusListIndex(String statusListId, Integer statusListIndex);

    List<Credential> findByIssuerWalletAddress(String issuerWalletAddress);

    @Query("SELECT c FROM Credential c WHERE c.issuedAt >= :fromDate")
    List<Credential> findCredentialsIssuedAfter(@Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT c FROM Credential c WHERE c.expiresAt <= :expiryDate AND c.status = 'VALID'")
    List<Credential> findExpiredCredentials(@Param("expiryDate") LocalDateTime expiryDate);

    @Query("SELECT COUNT(c) FROM Credential c WHERE c.statusListId = :statusListId")
    Long countByStatusListId(@Param("statusListId") String statusListId);

    boolean existsByCredentialId(String credentialId);

    boolean existsByIssuerDidAndHolderDid(String issuerDid, String holderDid);
}
