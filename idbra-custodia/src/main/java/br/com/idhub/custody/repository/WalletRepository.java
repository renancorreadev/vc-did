package br.com.idhub.custody.repository;

import br.com.idhub.custody.domain.Wallet;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends MongoRepository<Wallet, String> {

    /**
     * Busca carteira por endereço
     */
    Optional<Wallet> findByAddress(String address);

    /**
     * Busca carteiras ativas
     */
    List<Wallet> findByActiveTrue();

    /**
     * Busca carteiras por tipo
     */
    List<Wallet> findByWalletType(Wallet.WalletType walletType);

    /**
     * Verifica se endereço já existe
     */
    boolean existsByAddress(String address);

    /**
     * Busca carteiras por nome (case insensitive)
     */
    @Query("{'name': {$regex: ?0, $options: 'i'}, 'active': true}")
    List<Wallet> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Conta carteiras ativas
     */
    @Query(value = "{'active': true}", count = true)
    long countActiveWallets();

    /**
     * Busca carteiras criadas após uma data específica
     */
    @Query("{'createdAt': {$gte: ?0}, 'active': true}")
    List<Wallet> findWalletsCreatedAfter(@Param("date") java.time.LocalDateTime date);
}
