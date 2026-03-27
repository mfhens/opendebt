package dk.ufst.opendebt.personregistry.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.personregistry.service.EncryptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Seeds the persons table with fictional demo debtors whose UUIDs match the seed data in
 * debt-service (V1__baseline.sql) and case-service (V1__create_case_tables.sql).
 *
 * <p>Activated by setting {@code opendebt.demo.seed-persons=true}. Each entry is idempotent
 * (skipped if identifier_hash already exists). All PII is AES-256-GCM encrypted at rest.
 *
 * <p>UUIDs:
 *
 * <ul>
 *   <li>{@code d0000000-0000-0000-0000-000000000001} — Lars Andersen (SKAT, crossing debts)
 *   <li>{@code d0000000-0000-0000-0000-000000000002} — Mads Petersen (TOLD + underholdsbidrag)
 *   <li>{@code d0000000-0000-0000-0000-000000000003} — Emma Nielsen (SU-gæld + strafbøde)
 *   <li>{@code d0000000-0000-0000-0000-000000000004} — Jens Møller (ejendomsskat, disputed)
 * </ul>
 */
@Slf4j
@Component
@Order(2) // run after DemoOrganizationSeeder (order 1)
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "opendebt.demo.seed-persons",
    havingValue = "true",
    matchIfMissing = false)
public class DemoPersonSeeder implements ApplicationRunner {

  private static final String CPR_SALT = "CPR";

  private final JdbcTemplate jdbcTemplate;
  private final EncryptionService encryptionService;

  private static final String INSERT_PERSON =
      """
      INSERT INTO persons (
          id, identifier_encrypted, identifier_type, identifier_hash, role,
          name_encrypted, address_street_encrypted, address_city_encrypted,
          address_postal_code_encrypted, address_country_encrypted,
          email_encrypted, phone_encrypted,
          digital_post_enabled, eboks_enabled, access_count, last_accessed_at, data_retention_until)
      VALUES (?::uuid, ?, 'CPR', ?, 'PERSONAL', ?, ?, ?, ?, ?, ?, ?, true, true, 0, NOW(), CURRENT_DATE + INTERVAL '10 years')
      ON CONFLICT (identifier_hash, role) DO NOTHING
      """;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    int created = 0;
    for (PersonSeed seed : PERSONS) {
      String identifierHash = encryptionService.hash(seed.cpr, CPR_SALT);
      int rows =
          jdbcTemplate.update(
              INSERT_PERSON,
              seed.id,
              encryptionService.encrypt(seed.cpr),
              identifierHash,
              encryptionService.encrypt(seed.name),
              encryptionService.encrypt(seed.street),
              encryptionService.encrypt(seed.city),
              encryptionService.encrypt(seed.postalCode),
              encryptionService.encrypt("DK"),
              encryptionService.encrypt(seed.email),
              encryptionService.encrypt(seed.phone));
      created += rows;
    }
    log.info("Demo person seeder: created {} of {} persons", created, PERSONS.size());
  }

  private record PersonSeed(
      String id,
      String cpr,
      String name,
      String street,
      String city,
      String postalCode,
      String email,
      String phone) {}

  // @formatter:off
  // CPRs are purely fictional and do not correspond to real individuals.
  // UUIDs match debt-service V1__baseline.sql and case-service V1__create_case_tables.sql.
  private static final List<PersonSeed> PERSONS =
      List.of(
          // d...001: SKAT debts (c...001, c...004, A01, B01) — cases SAG-2025-00042
          new PersonSeed(
              "d0000000-0000-0000-0000-000000000001",
              "0503581234",
              "Lars Andersen",
              "Fredensgade 14",
              "K\u00f8benhavn N",
              "2200",
              "lars.andersen@mail.dk",
              "+45 28 11 22 33"),

          // d...002: TOLD debts (C01, C02) + underholdsbidrag (E01, E02) — cases SAG-2025-00099,
          // SAG-2026-00012
          new PersonSeed(
              "d0000000-0000-0000-0000-000000000002",
              "0208741234",
              "Mads Petersen",
              "Brogade 8",
              "Odense C",
              "5000",
              "mads.petersen@mail.dk",
              "+45 29 33 44 55"),

          // d...003: SU-gæld + strafbøde (D01, D02) — case SAG-2025-00103
          new PersonSeed(
              "d0000000-0000-0000-0000-000000000003",
              "2209961234",
              "Emma Nielsen",
              "Studiestr\u00e6de 22",
              "Aarhus C",
              "8000",
              "emma.nielsen@student.dk",
              "+45 31 55 66 77"),

          // d...004: EJENDOMSSKAT (c...005, disputed) — no case yet
          new PersonSeed(
              "d0000000-0000-0000-0000-000000000004",
              "3012571234",
              "Jens M\u00f8ller",
              "Strandvej 42",
              "Helsing\u00f8r",
              "3000",
              "jens.moller@mail.dk",
              "+45 40 77 88 99"));
  // @formatter:on
}
