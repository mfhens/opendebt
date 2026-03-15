package dk.ufst.opendebt.personregistry.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class PersonEntityTest {

  @Test
  void markAsDeleted_clearsPiiFieldsAndSetsReason() {
    PersonEntity entity =
        PersonEntity.builder()
            .id(UUID.randomUUID())
            .identifierEncrypted("enc".getBytes())
            .identifierType(PersonEntity.IdentifierType.CPR)
            .identifierHash("hash")
            .role(PersonEntity.PersonRole.PERSONAL)
            .nameEncrypted("name".getBytes())
            .addressStreetEncrypted("street".getBytes())
            .addressCityEncrypted("city".getBytes())
            .addressPostalCodeEncrypted("postal".getBytes())
            .addressCountryEncrypted("country".getBytes())
            .emailEncrypted("email".getBytes())
            .phoneEncrypted("phone".getBytes())
            .build();

    entity.markAsDeleted("GDPR erasure request");

    assertThat(entity.getNameEncrypted()).isNull();
    assertThat(entity.getAddressStreetEncrypted()).isNull();
    assertThat(entity.getAddressCityEncrypted()).isNull();
    assertThat(entity.getAddressPostalCodeEncrypted()).isNull();
    assertThat(entity.getAddressCountryEncrypted()).isNull();
    assertThat(entity.getEmailEncrypted()).isNull();
    assertThat(entity.getPhoneEncrypted()).isNull();
    assertThat(entity.getDeletedAt()).isNotNull();
    assertThat(entity.getDeletionReason()).isEqualTo("GDPR erasure request");
  }

  @Test
  void isDeleted_whenDeletedAtIsNull_returnsFalse() {
    PersonEntity entity = PersonEntity.builder().build();
    assertThat(entity.isDeleted()).isFalse();
  }

  @Test
  void isDeleted_afterMarkAsDeleted_returnsTrue() {
    PersonEntity entity = PersonEntity.builder().build();
    entity.markAsDeleted("test");
    assertThat(entity.isDeleted()).isTrue();
  }

  @Test
  void recordAccess_incrementsCountAndSetsTimestamp() {
    PersonEntity entity = PersonEntity.builder().accessCount(5L).build();

    entity.recordAccess();

    assertThat(entity.getAccessCount()).isEqualTo(6L);
    assertThat(entity.getLastAccessedAt()).isNotNull();
  }

  @Test
  void recordAccess_nullCount_initializesToOne() {
    PersonEntity entity = PersonEntity.builder().build();
    entity.setAccessCount(null);

    entity.recordAccess();

    assertThat(entity.getAccessCount()).isEqualTo(1L);
    assertThat(entity.getLastAccessedAt()).isNotNull();
  }
}
