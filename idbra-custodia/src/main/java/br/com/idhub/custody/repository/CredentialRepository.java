package br.com.idhub.custody.repository;

import br.com.idhub.custody.domain.Credential;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CredentialRepository extends MongoRepository<Credential, String> {

    Optional<Credential> findByCredentialId(String credentialId);

    List<Credential> findByIssuerDid(String issuerDid);

    List<Credential> findByHolderDid(String holderDid);

    List<Credential> findByStatus(String status);

    List<Credential> findByStatusListId(String statusListId);

    Optional<Credential> findByStatusListIdAndStatusListIndex(String statusListId, Integer statusListIndex);

    List<Credential> findByIssuerWalletAddress(String issuerWalletAddress);

    @Query("{'issuedAt': {$gte: ?0}}")
    List<Credential> findCredentialsIssuedAfter(@Param("fromDate") LocalDateTime fromDate);

    @Query("{'expiresAt': {$lte: ?0}, 'status': 'VALID'}")
    List<Credential> findExpiredCredentials(@Param("expiryDate") LocalDateTime expiryDate);

    @Query(value = "{'statusListId': ?0}", count = true)
    Long countByStatusListId(@Param("statusListId") String statusListId);

    boolean existsByCredentialId(String credentialId);

    boolean existsByIssuerDidAndHolderDid(String issuerDid, String holderDid);
}
