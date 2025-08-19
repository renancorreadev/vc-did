package br.com.idhub.custody.service;

import br.com.idhub.custody.domain.Wallet;
import br.com.idhub.custody.domain.WalletMetadata;
import br.com.idhub.custody.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class WalletService {

    private final Web3j web3j;
    private final Credentials credentials;
    private final WalletRepository walletRepository;
    private final CryptoService cryptoService;
    private final SecureRandom secureRandom = new SecureRandom();

    public WalletService(Web3j web3j, Credentials credentials, WalletRepository walletRepository, CryptoService cryptoService) {
        this.web3j = web3j;
        this.credentials = credentials;
        this.walletRepository = walletRepository;
        this.cryptoService = cryptoService;
    }

    /**
     * Cria uma nova carteira Ethereum de forma segura
     */
    public WalletMetadata createWallet(String name, String description) {
        try {
            // Gerar par de chaves criptográficas usando Web3j
            ECKeyPair keyPair = Keys.createEcKeyPair();

            // Obter endereço da carteira e garantir formato correto
            String address = Keys.getAddress(keyPair);
            if (!address.startsWith("0x")) {
                address = "0x" + address;
            }

            // Converter chave privada para formato hexadecimal
            String privateKey = Numeric.toHexStringWithPrefix(keyPair.getPrivateKey());

            // Criptografar a chave privada
            String encryptedPrivateKey = cryptoService.encryptWithMasterPassword(privateKey);
            String salt = cryptoService.generateSalt();

            // Criar entidade Wallet para persistência
            Wallet wallet = new Wallet(address, name, description, encryptedPrivateKey, salt);
            wallet.setWalletType(Wallet.WalletType.ETHEREUM);

            // Salvar no banco de dados
            Wallet savedWallet = walletRepository.save(wallet);

            // Retornar metadados (sem a chave privada)
            return new WalletMetadata(
                savedWallet.getAddress(),
                savedWallet.getName(),
                savedWallet.getDescription(),
                savedWallet.getCreatedAt(),
                savedWallet.getUpdatedAt(),
                savedWallet.isActive()
            );

        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar carteira: " + e.getMessage(), e);
        }
    }

    /**
     * Cria carteira com senha personalizada
     */
    public WalletMetadata createWalletWithPassword(String name, String description, String password) {
        try {
            // Gerar par de chaves criptográficas
            ECKeyPair keyPair = Keys.createEcKeyPair();
            String address = Keys.getAddress(keyPair);

            // Garantir formato correto do endereço
            if (!address.startsWith("0x")) {
                address = "0x" + address;
            }

            String privateKey = Numeric.toHexStringWithPrefix(keyPair.getPrivateKey());

            // Criptografar com senha personalizada
            String encryptedPrivateKey = cryptoService.encryptPrivateKey(privateKey, password);
            String salt = cryptoService.generateSalt();

            // Salvar carteira
            Wallet wallet = new Wallet(address, name, description, encryptedPrivateKey, salt);
            wallet.setWalletType(Wallet.WalletType.ETHEREUM);

            Wallet savedWallet = walletRepository.save(wallet);

            return new WalletMetadata(
                savedWallet.getAddress(),
                savedWallet.getName(),
                savedWallet.getDescription(),
                savedWallet.getCreatedAt(),
                savedWallet.getUpdatedAt(),
                savedWallet.isActive()
            );

        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar carteira com senha: " + e.getMessage(), e);
        }
    }

    /**
     * Obtém informações da carteira
     */
    public WalletMetadata getWalletInfo(String address) {
        Optional<Wallet> walletOpt = walletRepository.findByAddress(address);
        if (walletOpt.isPresent()) {
            Wallet wallet = walletOpt.get();
            return new WalletMetadata(
                wallet.getAddress(),
                wallet.getName(),
                wallet.getDescription(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt(),
                wallet.isActive()
            );
        }
        throw new RuntimeException("Carteira não encontrada: " + address);
    }

    /**
     * Lista todas as carteiras ativas
     */
    public List<WalletMetadata> getAllWallets() {
        List<Wallet> wallets = walletRepository.findByActiveTrue();
        return wallets.stream()
            .map(wallet -> new WalletMetadata(
                wallet.getAddress(),
                wallet.getName(),
                wallet.getDescription(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt(),
                wallet.isActive()
            ))
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Obtém credenciais para uma carteira específica
     */
    public Credentials getWalletCredentials(String address, String password) {
        try {
            Optional<Wallet> walletOpt = walletRepository.findByAddress(address);
            if (walletOpt.isPresent()) {
                Wallet wallet = walletOpt.get();
                String decryptedPrivateKey = cryptoService.decryptPrivateKey(wallet.getEncryptedPrivateKey(), password);
                Credentials credentials = Credentials.create(decryptedPrivateKey);

                // Garantir que o endereço retornado tenha formato correto
                String walletAddress = credentials.getAddress();
                if (!walletAddress.startsWith("0x")) {
                    walletAddress = "0x" + walletAddress;
                }

                return Credentials.create(decryptedPrivateKey);
            }
            throw new RuntimeException("Carteira não encontrada: " + address);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter credenciais: " + e.getMessage(), e);
        }
    }

    /**
     * Obtém credenciais usando senha mestra
     */
    public Credentials getWalletCredentialsWithMasterPassword(String address) {
        try {
            Optional<Wallet> walletOpt = walletRepository.findByAddress(address);
            if (walletOpt.isPresent()) {
                Wallet wallet = walletOpt.get();
                String decryptedPrivateKey = cryptoService.decryptWithMasterPassword(wallet.getEncryptedPrivateKey());
                Credentials credentials = Credentials.create(decryptedPrivateKey);

                // Garantir que o endereço retornado tenha formato correto
                String walletAddress = credentials.getAddress();
                if (!walletAddress.startsWith("0x")) {
                    walletAddress = "0x" + walletAddress;
                }

                return Credentials.create(decryptedPrivateKey);
            }
            throw new RuntimeException("Carteira não encontrada: " + address);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter credenciais: " + e.getMessage(), e);
        }
    }

    /**
     * Obtém saldo da carteira
     */
    public CompletableFuture<BigInteger> getBalance(String address) {
        return web3j.ethGetBalance(address, null)
                .sendAsync()
                .thenApply(EthGetBalance::getBalance);
    }

    /**
     * Obtém saldo formatado
     */
    public String getFormattedBalance(String address) {
        try {
            BigInteger balance = getBalance(address).get();
            return Convert.fromWei(balance.toString(), Convert.Unit.ETHER).toString() + " ETH";
        } catch (Exception e) {
            return "Erro ao obter saldo: " + e.getMessage();
        }
    }

    /**
     * Obtém credenciais padrão (para compatibilidade)
     */
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Desativa uma carteira
     */
    public boolean deactivateWallet(String address) {
        Optional<Wallet> walletOpt = walletRepository.findByAddress(address);
        if (walletOpt.isPresent()) {
            Wallet wallet = walletOpt.get();
            wallet.setActive(false);
            walletRepository.save(wallet);
            return true;
        }
        return false;
    }

    /**
     * Atualiza informações da carteira
     */
    public WalletMetadata updateWallet(String address, String name, String description) {
        Optional<Wallet> walletOpt = walletRepository.findByAddress(address);
        if (walletOpt.isPresent()) {
            Wallet wallet = walletOpt.get();
            wallet.setName(name);
            wallet.setDescription(description);
            Wallet updatedWallet = walletRepository.save(wallet);

            return new WalletMetadata(
                updatedWallet.getAddress(),
                updatedWallet.getName(),
                updatedWallet.getDescription(),
                updatedWallet.getCreatedAt(),
                updatedWallet.getUpdatedAt(),
                updatedWallet.isActive()
            );
        }
        throw new RuntimeException("Carteira não encontrada: " + address);
    }

    // ⚠️ APENAS PARA DESENVOLVIMENTO - Obter chave privada descriptografada
    public String getPrivateKeyForDevelopment(String address) {
        try {
            Optional<Wallet> walletOpt = walletRepository.findByAddress(address);
            if (walletOpt.isPresent()) {
                Wallet wallet = walletOpt.get();
                String decryptedPrivateKey = cryptoService.decryptWithMasterPassword(wallet.getEncryptedPrivateKey());
                return decryptedPrivateKey;
            }
            throw new RuntimeException("Carteira não encontrada: " + address);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter chave privada: " + e.getMessage(), e);
        }
    }

    // ⚠️ APENAS PARA DESENVOLVIMENTO - Obter chave privada criptografada
    public String getEncryptedPrivateKeyForDevelopment(String address) {
        try {
            Optional<Wallet> walletOpt = walletRepository.findByAddress(address);
            if (walletOpt.isPresent()) {
                Wallet wallet = walletOpt.get();
                return wallet.getEncryptedPrivateKey();
            }
            throw new RuntimeException("Carteira não encontrada: " + address);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter chave privada criptografada: " + e.getMessage(), e);
        }
    }

    /**
     * Obter credenciais da wallet para operações blockchain (sem senha)
     */
    public Credentials getWalletCredentialsForBlockchain(String walletAddress) throws Exception {
        Optional<Wallet> walletOpt = walletRepository.findByAddress(walletAddress);
        if (walletOpt.isEmpty()) {
            throw new RuntimeException("Wallet não encontrada: " + walletAddress);
        }

        Wallet wallet = walletOpt.get();
        if (!wallet.isActive()) {
            throw new RuntimeException("Wallet inativa: " + walletAddress);
        }

        // Para operações blockchain, usamos a senha master do sistema
        String masterPassword = System.getenv("CRYPTO_MASTER_PASSWORD");
        if (masterPassword == null || masterPassword.isEmpty()) {
            throw new RuntimeException("CRYPTO_MASTER_PASSWORD não configurada");
        }

        // Descriptografar chave privada
        String privateKeyHex = cryptoService.decryptPrivateKey(wallet.getEncryptedPrivateKey(), masterPassword);

        // Criar credenciais Web3j
        return Credentials.create(privateKeyHex);
    }

    /**
     * Verificar se uma wallet existe
     */
    public boolean walletExists(String walletAddress) {
        return walletRepository.findByAddress(walletAddress).isPresent();
    }

    /**
     * Importar wallet a partir de chave privada (para uso administrativo)
     */
    public WalletMetadata importWalletFromPrivateKey(String privateKeyHex, String name, String description) {
        try {
            // Remover prefixo 0x se existir
            if (privateKeyHex.startsWith("0x")) {
                privateKeyHex = privateKeyHex.substring(2);
            }

            // Criar credenciais a partir da chave privada
            Credentials credentials = Credentials.create(privateKeyHex);
            String address = credentials.getAddress();

            // Verificar se a wallet já existe
            if (walletExists(address)) {
                throw new RuntimeException("Wallet já existe: " + address);
            }

            // Obter senha master para criptografia
            String masterPassword = System.getenv("CRYPTO_MASTER_PASSWORD");
            if (masterPassword == null || masterPassword.isEmpty()) {
                throw new RuntimeException("CRYPTO_MASTER_PASSWORD não configurada");
            }

            // Criptografar chave privada
            String encryptedPrivateKey = cryptoService.encryptPrivateKey(privateKeyHex, masterPassword);

            // Gerar salt para criptografia (CORREÇÃO: estava faltando!)
            String encryptionSalt = cryptoService.generateSalt();

            // Criar entidade Wallet
            Wallet wallet = new Wallet();
            wallet.setAddress(address);
            wallet.setName(name);
            wallet.setDescription(description);
            wallet.setEncryptedPrivateKey(encryptedPrivateKey);
            wallet.setEncryptionSalt(encryptionSalt); // CORREÇÃO: definir o salt
            wallet.setActive(true);

            // Salvar no banco
            Wallet savedWallet = walletRepository.save(wallet);

            // Retornar metadata
            return new WalletMetadata(
                savedWallet.getAddress(),
                savedWallet.getName(),
                savedWallet.getDescription()
            );

        } catch (Exception e) {
            throw new RuntimeException("Erro ao importar wallet: " + e.getMessage(), e);
        }
    }

    /**
     * Obter credenciais administrativas a partir da chave privada configurada no ambiente
     */
    public Credentials getAdminCredentials() {
        String adminPrivateKey = System.getenv("ADMIN_PRIVATE_KEY");

        if (adminPrivateKey == null || adminPrivateKey.isEmpty()) {
            throw new RuntimeException("ADMIN_PRIVATE_KEY não configurada nas variáveis de ambiente");
        }

        // Remover prefixo 0x se existir
        if (adminPrivateKey.startsWith("0x")) {
            adminPrivateKey = adminPrivateKey.substring(2);
        }

        return Credentials.create(adminPrivateKey);
    }

    /**
     * Importar wallet administrativa a partir da chave privada configurada no ambiente
     */
    public WalletMetadata importAdminWalletFromEnv(String name, String description) {
        String adminPrivateKey = System.getenv("ADMIN_PRIVATE_KEY");

        if (adminPrivateKey == null || adminPrivateKey.isEmpty()) {
            throw new RuntimeException("ADMIN_PRIVATE_KEY não configurada nas variáveis de ambiente");
        }

        return importWalletFromPrivateKey(adminPrivateKey, name, description);
    }
}
