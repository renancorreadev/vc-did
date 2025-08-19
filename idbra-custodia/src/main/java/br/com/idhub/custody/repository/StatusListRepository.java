package br.com.idhub.custody.repository;

import br.com.idhub.custody.domain.StatusList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StatusListRepository extends JpaRepository<StatusList, Long> {

    Optional<StatusList> findByListId(String listId);

    List<StatusList> findByIssuer(String issuer);

    List<StatusList> findByPurpose(String purpose);

    List<StatusList> findByIssuerWalletAddress(String issuerWalletAddress);

    @Query("SELECT sl FROM StatusList sl WHERE sl.version = (SELECT MAX(sl2.version) FROM StatusList sl2 WHERE sl2.listId = :listId)")
    Optional<StatusList> findLatestVersionByListId(@Param("listId") String listId);

    @Query("SELECT sl FROM StatusList sl WHERE sl.listId = :listId ORDER BY sl.version DESC")
    List<StatusList> findAllVersionsByListId(@Param("listId") String listId);

    boolean existsByListId(String listId);

    @Query("SELECT COUNT(sl) FROM StatusList sl WHERE sl.issuer = :issuer")
    Long countByIssuer(@Param("issuer") String issuer);

    @Query("SELECT COUNT(s) FROM StatusList s WHERE s.listId = :listId")
    Long countByListId(@Param("listId") String listId);
}
