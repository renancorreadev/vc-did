package br.com.idhub.custody.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.GCMParameterSpec;
import java.security.spec.KeySpec;

@Service
public class CryptoService {

    @Value("${crypto.master.password:defaultMasterPassword123!}")
    private String masterPassword;

    @Value("${crypto.algorithm:AES}")
    private String algorithm;

    @Value("${crypto.key.length:256}")
    private int keyLength;

    @Value("${crypto.iterations:65536}")
    private int iterations;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Criptografa uma chave privada usando uma senha mestra
     */
    public String encryptPrivateKey(String privateKey, String password) throws Exception {
        // Gerar salt único para esta carteira
        String salt = generateSalt();

        // Derivar chave de criptografia da senha mestra + salt
        SecretKey secretKey = deriveKey(password + salt, salt);

        // Criptografar a chave privada
        Cipher cipher = Cipher.getInstance(algorithm + "/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encryptedBytes = cipher.doFinal(privateKey.getBytes());
        byte[] iv = cipher.getIV();

        // Combinar IV + dados criptografados + salt
        byte[] combined = new byte[iv.length + encryptedBytes.length + salt.getBytes().length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);
        System.arraycopy(salt.getBytes(), 0, combined, iv.length + encryptedBytes.length, salt.getBytes().length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Descriptografa uma chave privada
     */
    public String decryptPrivateKey(String encryptedData, String password) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedData);

        // Extrair IV, dados criptografados e salt
        int ivLength = 12; // GCM IV length
        int saltLength = 44; // Salt length (32 bytes = 44 chars em base64)

        if (combined.length < ivLength + saltLength) {
            throw new IllegalArgumentException("Dados criptografados inválidos");
        }

        byte[] iv = new byte[ivLength];
        byte[] encryptedBytes = new byte[combined.length - ivLength - saltLength];
        byte[] saltBytes = new byte[saltLength];

        System.arraycopy(combined, 0, iv, 0, ivLength);
        System.arraycopy(combined, ivLength, encryptedBytes, 0, encryptedBytes.length);
        System.arraycopy(combined, ivLength + encryptedBytes.length, saltBytes, 0, saltLength);

        String salt = new String(saltBytes);

        // Derivar chave de descriptografia
        SecretKey secretKey = deriveKey(password + salt, salt);

        // Descriptografar
        Cipher cipher = Cipher.getInstance(algorithm + "/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));

        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes);
    }

    /**
     * Gera um salt único para cada carteira
     */
    public String generateSalt() {
        byte[] salt = new byte[32];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Deriva uma chave de criptografia usando PBKDF2
     */
    private SecretKey deriveKey(String password, String salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), iterations, keyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, algorithm);
    }

    /**
     * Criptografa usando a senha mestra padrão
     */
    public String encryptWithMasterPassword(String privateKey) throws Exception {
        return encryptPrivateKey(privateKey, masterPassword);
    }

    /**
     * Descriptografa usando a senha mestra padrão
     */
    public String decryptWithMasterPassword(String encryptedData) throws Exception {
        return decryptPrivateKey(encryptedData, masterPassword);
    }
}
