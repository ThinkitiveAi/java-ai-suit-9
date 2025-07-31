package com.example.session_demo.service;

import java.util.List;

/**
 * Service for encrypting and decrypting sensitive medical data
 * Provides AES-256 encryption for HIPAA compliance
 */
public interface EncryptionService {
    
    /**
     * Encrypt a string value
     */
    String encrypt(String plainText);
    
    /**
     * Decrypt an encrypted string value
     */
    String decrypt(String encryptedText);
    
    /**
     * Encrypt a list of strings (for medical history, allergies, etc.)
     */
    String encryptList(List<String> plainTextList);
    
    /**
     * Decrypt a list of strings
     */
    List<String> decryptList(String encryptedText);
    
    /**
     * Generate a secure hash (one-way encryption)
     */
    String generateHash(String plainText);
    
    /**
     * Verify a hash matches the original text
     */
    boolean verifyHash(String plainText, String hash);
} 