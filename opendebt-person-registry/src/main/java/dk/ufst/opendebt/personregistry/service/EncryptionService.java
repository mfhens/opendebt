package dk.ufst.opendebt.personregistry.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for encrypting and decrypting PII data. Uses AES-256-GCM for authenticated encryption.
 */
@Slf4j
@Service
public class EncryptionService {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;

  private final SecretKey secretKey;

  public EncryptionService(@Value("${opendebt.encryption.key}") String base64Key) {
    byte[] keyBytes = Base64.getDecoder().decode(base64Key);
    if (keyBytes.length != 32) {
      throw new IllegalArgumentException("Encryption key must be 256 bits (32 bytes)");
    }
    this.secretKey = new SecretKeySpec(keyBytes, "AES");
  }

  /**
   * Encrypts a plaintext string.
   *
   * @param plaintext The text to encrypt
   * @return Encrypted bytes (IV prepended)
   */
  public byte[] encrypt(String plaintext) {
    if (plaintext == null) {
      return null;
    }
    try {
      byte[] iv = new byte[GCM_IV_LENGTH];
      new SecureRandom().nextBytes(iv);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

      // Prepend IV to ciphertext
      byte[] result = new byte[iv.length + ciphertext.length];
      System.arraycopy(iv, 0, result, 0, iv.length);
      System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);

      return result;
    } catch (Exception e) {
      log.error("Encryption failed", e);
      throw new RuntimeException("Encryption failed", e);
    }
  }

  /**
   * Decrypts encrypted bytes back to plaintext.
   *
   * @param encrypted The encrypted bytes (IV prepended)
   * @return Decrypted plaintext
   */
  public String decrypt(byte[] encrypted) {
    if (encrypted == null) {
      return null;
    }
    try {
      // Extract IV from beginning
      byte[] iv = new byte[GCM_IV_LENGTH];
      System.arraycopy(encrypted, 0, iv, 0, iv.length);

      // Extract ciphertext
      byte[] ciphertext = new byte[encrypted.length - iv.length];
      System.arraycopy(encrypted, iv.length, ciphertext, 0, ciphertext.length);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

      byte[] plaintext = cipher.doFinal(ciphertext);
      return new String(plaintext, StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.error("Decryption failed", e);
      throw new RuntimeException("Decryption failed", e);
    }
  }

  /**
   * Creates a SHA-256 hash of the input for lookup purposes. The hash is salted with the identifier
   * type to prevent rainbow table attacks.
   *
   * @param value The value to hash
   * @param salt Additional salt (e.g., identifier type)
   * @return Hex-encoded hash
   */
  public String hash(String value, String salt) {
    if (value == null) {
      return null;
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(salt.getBytes(StandardCharsets.UTF_8));
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      return bytesToHex(hash);
    } catch (Exception e) {
      log.error("Hashing failed", e);
      throw new RuntimeException("Hashing failed", e);
    }
  }

  private String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
