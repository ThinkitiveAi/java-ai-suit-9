package com.example.session_demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EncryptionServiceImpl implements EncryptionService {

    private final ObjectMapper objectMapper;

    @Value("${security.encryption.key:default-encryption-key-change-in-production}")
    private String encryptionKey;

    @Value("${security.encryption.algorithm:AES}")
    private String algorithm;

    @Value("${security.encryption.transformation:AES/GCM/NoPadding}")
    private String transformation;

    @Value("${security.encryption.iv-length:12}")
    private int ivLength;

    @Value("${security.encryption.tag-length:16}")
    private int tagLength;

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 32;

    @Override
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }

        try {
            // Generate a random IV for each encryption
            SecureRandom secureRandom = new SecureRandom();
            byte[] iv = new byte[ivLength];
            secureRandom.nextBytes(iv);

            // Create cipher
            Cipher cipher = Cipher.getInstance(transformation);
            SecretKeySpec keySpec = new SecretKeySpec(getKeyBytes(), algorithm);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(tagLength * 8, iv);
            
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            // Encrypt the data
            byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Combine IV and encrypted data
            byte[] combined = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);

            // Return Base64 encoded result
            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    @Override
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return null;
        }

        try {
            // Decode from Base64
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            // Extract IV and encrypted data
            byte[] iv = new byte[ivLength];
            byte[] encryptedData = new byte[combined.length - ivLength];
            System.arraycopy(combined, 0, iv, 0, ivLength);
            System.arraycopy(combined, ivLength, encryptedData, 0, encryptedData.length);

            // Create cipher
            Cipher cipher = Cipher.getInstance(transformation);
            SecretKeySpec keySpec = new SecretKeySpec(getKeyBytes(), algorithm);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(tagLength * 8, iv);
            
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            // Decrypt the data
            byte[] decryptedData = cipher.doFinal(encryptedData);

            return new String(decryptedData, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    @Override
    public String encryptList(List<String> plainTextList) {
        if (plainTextList == null || plainTextList.isEmpty()) {
            return null;
        }

        try {
            // Convert list to JSON string
            String jsonString = objectMapper.writeValueAsString(plainTextList);
            return encrypt(jsonString);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize list for encryption", e);
            throw new RuntimeException("Failed to serialize list for encryption", e);
        }
    }

    @Override
    public List<String> decryptList(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return null;
        }

        try {
            // Decrypt to JSON string
            String jsonString = decrypt(encryptedText);
            
            if (jsonString == null) {
                return null;
            }

            // Convert JSON string back to list
            return objectMapper.readValue(jsonString, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize decrypted list", e);
            throw new RuntimeException("Failed to deserialize decrypted list", e);
        }
    }

    @Override
    public String generateHash(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }

        try {
            // Generate a random salt
            SecureRandom secureRandom = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            secureRandom.nextBytes(salt);

            // Create hash with salt
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] hashedBytes = md.digest(plainText.getBytes(StandardCharsets.UTF_8));

            // Combine salt and hash
            byte[] combined = new byte[salt.length + hashedBytes.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hashedBytes, 0, combined, salt.length, hashedBytes.length);

            return Base64.getEncoder().encodeToString(combined);

        } catch (NoSuchAlgorithmException e) {
            log.error("Hash generation failed", e);
            throw new RuntimeException("Failed to generate hash", e);
        }
    }

    @Override
    public boolean verifyHash(String plainText, String hash) {
        if (plainText == null || hash == null) {
            return false;
        }

        try {
            // Decode the stored hash
            byte[] combined = Base64.getDecoder().decode(hash);

            // Extract salt and hash
            byte[] salt = new byte[SALT_LENGTH];
            byte[] storedHash = new byte[combined.length - SALT_LENGTH];
            System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);
            System.arraycopy(combined, SALT_LENGTH, storedHash, 0, storedHash.length);

            // Generate hash for the provided plain text with the same salt
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] computedHash = md.digest(plainText.getBytes(StandardCharsets.UTF_8));

            // Compare hashes
            return MessageDigest.isEqual(storedHash, computedHash);

        } catch (Exception e) {
            log.error("Hash verification failed", e);
            return false;
        }
    }

    /**
     * Get the encryption key as bytes, ensuring it's the right length for AES-256
     */
    private byte[] getKeyBytes() {
        try {
            // Use SHA-256 to ensure we have a 256-bit key
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            return md.digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }

    /**
     * Generate a new AES key (for key rotation)
     */
    public String generateNewKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
            keyGen.init(256); // AES-256
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate new encryption key", e);
        }
    }
} 