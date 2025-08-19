package br.com.idhub.custody.repository;

import br.com.idhub.custody.domain.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

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
    @Query("SELECT w FROM Wallet w WHERE LOWER(w.name) LIKE LOWER(CONCAT('%', :name, '%')) AND w.active = true")
    List<Wallet> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Conta carteiras ativas
     */
    @Query("SELECT COUNT(w) FROM Wallet w WHERE w.active = true")
    long countActiveWallets();

    /**
     * Busca carteiras criadas após uma data específica
     */
    @Query("SELECT w FROM Wallet w WHERE w.createdAt >= :date AND w.active = true")
    List<Wallet> findWalletsCreatedAfter(@Param("date") java.time.LocalDateTime date);
}
