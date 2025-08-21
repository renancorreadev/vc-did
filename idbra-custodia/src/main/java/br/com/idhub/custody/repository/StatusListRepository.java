package br.com.idhub.custody.repository;

import br.com.idhub.custody.domain.StatusList;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatusListRepository extends MongoRepository<StatusList, String> {

    Optional<StatusList> findByListId(String listId);

    List<StatusList> findByIssuer(String issuer);

    List<StatusList> findByPurpose(String purpose);

    List<StatusList> findByIssuerWalletAddress(String issuerWalletAddress);

    @Query("{'listId': ?0}")
    Optional<StatusList> findLatestVersionByListId(@Param("listId") String listId);

    @Query("{'listId': ?0}")
    List<StatusList> findAllVersionsByListId(@Param("listId") String listId);

    boolean existsByListId(String listId);

    @Query(value = "{'issuer': ?0}", count = true)
    Long countByIssuer(@Param("issuer") String issuer);

    @Query(value = "{'listId': ?0}", count = true)
    Long countByListId(@Param("listId") String listId);
}
