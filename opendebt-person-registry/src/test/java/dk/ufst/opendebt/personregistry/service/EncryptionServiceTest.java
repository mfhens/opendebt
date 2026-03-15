package dk.ufst.opendebt.personregistry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dk.ufst.opendebt.personregistry.service.EncryptionService.EncryptionOperationException;

class EncryptionServiceTest {

  private static final String VALID_KEY =
      Base64.getEncoder().encodeToString("abcdefghijklmnopqrstuvwxyz123456".getBytes());

  private EncryptionService encryptionService;

  @BeforeEach
  void setUp() {
    encryptionService = new EncryptionService(VALID_KEY);
  }

  @Test
  void constructor_invalidKeyLength_throwsException() {
    String shortKey = Base64.getEncoder().encodeToString("short".getBytes());
    assertThatThrownBy(() -> new EncryptionService(shortKey))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("256 bits");
  }

  @Test
  void encrypt_null_returnsEmptyBytes() {
    byte[] result = encryptionService.encrypt(null);
    assertThat(result).isEmpty();
  }

  @Test
  void encrypt_validPlaintext_returnsNonEmptyBytes() {
    byte[] result = encryptionService.encrypt("hello");
    assertThat(result).isNotEmpty();
    assertThat(result.length).isGreaterThan(12); // IV (12) + ciphertext
  }

  @Test
  void decrypt_null_returnsNull() {
    String result = encryptionService.decrypt(null);
    assertThat(result).isNull();
  }

  @Test
  void decrypt_emptyArray_returnsNull() {
    String result = encryptionService.decrypt(new byte[0]);
    assertThat(result).isNull();
  }

  @Test
  void decrypt_tooShort_throwsException() {
    byte[] tooShort = new byte[10];
    assertThatThrownBy(() -> encryptionService.decrypt(tooShort))
        .isInstanceOf(EncryptionOperationException.class)
        .hasMessageContaining("too short");
  }

  @Test
  void encryptAndDecrypt_roundtrip_returnsOriginal() {
    String plaintext = "test-data-with-special-chars-æøå";
    byte[] encrypted = encryptionService.encrypt(plaintext);
    String decrypted = encryptionService.decrypt(encrypted);
    assertThat(decrypted).isEqualTo(plaintext);
  }

  @Test
  void encrypt_samePlaintext_producesDifferentCiphertexts() {
    byte[] first = encryptionService.encrypt("same");
    byte[] second = encryptionService.encrypt("same");
    assertThat(first).isNotEqualTo(second);
  }

  @Test
  void hash_null_returnsNull() {
    String result = encryptionService.hash(null, "SALT");
    assertThat(result).isNull();
  }

  @Test
  void hash_validInput_returnsConsistentHash() {
    String hash1 = encryptionService.hash("1234567890", "CPR");
    String hash2 = encryptionService.hash("1234567890", "CPR");
    assertThat(hash1).isEqualTo(hash2);
    assertThat(hash1).hasSize(64); // SHA-256 hex
  }

  @Test
  void hash_differentSalts_returnDifferentHashes() {
    String hashCpr = encryptionService.hash("1234567890", "CPR");
    String hashCvr = encryptionService.hash("1234567890", "CVR");
    assertThat(hashCpr).isNotEqualTo(hashCvr);
  }

  @Test
  void hash_differentValues_returnDifferentHashes() {
    String hash1 = encryptionService.hash("1234567890", "CPR");
    String hash2 = encryptionService.hash("0987654321", "CPR");
    assertThat(hash1).isNotEqualTo(hash2);
  }
}
