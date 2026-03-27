package dk.ufst.opendebt.personregistry.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dk.ufst.opendebt.personregistry.entity.OrganizationEntity.OrganizationType;
import dk.ufst.opendebt.personregistry.service.EncryptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Seeds the organizations table with core public-sector fordringshavere from gaeldst.dk. Activated
 * by setting {@code opendebt.demo.seed-organizations=true}. Each entry is idempotent (skipped if
 * cvr_hash already exists). UUIDs are deterministic and match the creditor-service seed migration
 * V4__seed_core_public_sector_creditors.sql.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "opendebt.demo.seed-organizations",
    havingValue = "true",
    matchIfMissing = false)
public class DemoOrganizationSeeder implements ApplicationRunner {

  private final JdbcTemplate jdbcTemplate;
  private final EncryptionService encryptionService;

  private static final String INSERT_ORG =
      """
      INSERT INTO organizations (id, cvr_encrypted, cvr_hash, name_encrypted, organization_type, active)
      VALUES (?::uuid, ?, ?, ?, ?, true)
      ON CONFLICT (id) DO NOTHING
      """;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    int created = 0;
    for (OrgSeed seed : ORGANIZATIONS) {
      String cvrHash = encryptionService.hash(seed.cvr, "CVR");
      byte[] cvrEncrypted = encryptionService.encrypt(seed.cvr);
      byte[] nameEncrypted = encryptionService.encrypt(seed.name);
      int rows =
          jdbcTemplate.update(
              INSERT_ORG, seed.id, cvrEncrypted, cvrHash, nameEncrypted, seed.type.name());
      created += rows;
    }
    log.info(
        "Demo organization seeder: created {} of {} organizations", created, ORGANIZATIONS.size());
  }

  private record OrgSeed(String id, String cvr, String name, OrganizationType type) {}

  // @formatter:off
  private static final List<OrgSeed> ORGANIZATIONS =
      List.of(
          // ── Demo creditor orgs (match debt-service V1__baseline.sql seed UUIDs) ───────────
          new OrgSeed(
              "00000000-0000-0000-0000-000000000001",
              "99000001",
              "Skattestyrelsen (Demo)",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "00000000-0000-0000-0000-000000000002",
              "99000002",
              "Toldstyrelsen (Demo)",
              OrganizationType.STATE_AGENCY),

          // ── Kommuner (98) ─────────────────────────────────────────────────
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000001",
              "11000001",
              "Albertslund Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000002",
              "11000002",
              "Aller\u00f8d Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000003",
              "11000003",
              "Assens Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000004",
              "11000004",
              "Ballerup Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000005",
              "11000005",
              "Billund Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000006",
              "11000006",
              "Bornholms Regionskommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000007",
              "11000007",
              "Br\u00f8ndby Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000008",
              "11000008",
              "Br\u00f8nderslev Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000009",
              "11000009",
              "Drag\u00f8r Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000010",
              "11000010",
              "Egedal Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000011",
              "11000011",
              "Esbjerg Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000012",
              "11000012",
              "Fan\u00f8 Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000013",
              "11000013",
              "Favrskov Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000014",
              "11000014",
              "Faxe Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000015",
              "11000015",
              "Fredensborg Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000016",
              "11000016",
              "Fredericia Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000017",
              "11000017",
              "Frederiksberg Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000018",
              "11000018",
              "Frederikshavn Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000019",
              "11000019",
              "Frederikssund Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000020",
              "11000020",
              "Fures\u00f8 Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000021",
              "11000021",
              "Faaborg-Midtfyn Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000022",
              "11000022",
              "Gentofte Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000023",
              "11000023",
              "Gladsaxe Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000024",
              "11000024",
              "Glostrup Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000025",
              "11000025",
              "Greve Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000026",
              "11000026",
              "Gribskov Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000027",
              "11000027",
              "Guldborgsund Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000028",
              "11000028",
              "Haderslev Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000029",
              "11000029",
              "Halsn\u00e6s Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000030",
              "11000030",
              "Hedensted Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000031",
              "11000031",
              "Helsing\u00f8r Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000032",
              "11000032",
              "Herlev Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000033",
              "11000033",
              "Herning Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000034",
              "11000034",
              "Hiller\u00f8d Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000035",
              "11000035",
              "Hj\u00f8rring Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000036",
              "11000036",
              "Holb\u00e6k Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000037",
              "11000037",
              "Holstebro Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000038",
              "11000038",
              "Horsens Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000039",
              "11000039",
              "Hvidovre Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000040",
              "11000040",
              "H\u00f8je-Taastrup Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000041",
              "11000041",
              "H\u00f8rsholm Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000042",
              "11000042",
              "Ikast-Brande Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000043",
              "11000043",
              "Ish\u00f8j Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000044",
              "11000044",
              "Jammerbugt Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000045",
              "11000045",
              "Kalundborg Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000046",
              "11000046",
              "Kerteminde Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000047",
              "11000047",
              "Kolding Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000048",
              "11000048",
              "K\u00f8benhavns Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000049",
              "11000049",
              "K\u00f8ge Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000050",
              "11000050",
              "Langeland Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000051",
              "11000051",
              "Lejre Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000052",
              "11000052",
              "Lemvig Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000053",
              "11000053",
              "Lolland Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000054",
              "11000054",
              "Lyngby-Taarb\u00e6k Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000055",
              "11000055",
              "L\u00e6s\u00f8 Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000056",
              "11000056",
              "Mariagerfjord Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000057",
              "11000057",
              "Middelfart Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000058",
              "11000058",
              "Mors\u00f8 Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000059",
              "11000059",
              "Norddjurs Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000060",
              "11000060",
              "Nordfyns Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000061",
              "11000061",
              "Nyborg Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000062",
              "11000062",
              "N\u00e6stved Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000063",
              "11000063",
              "Odder Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000064",
              "11000064",
              "Odense Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000065",
              "11000065",
              "Odsherred Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000066",
              "11000066",
              "Randers Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000067",
              "11000067",
              "Rebild Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000068",
              "11000068",
              "Ringk\u00f8bing-Skjern Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000069",
              "11000069",
              "Ringsted Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000070",
              "11000070",
              "Roskilde Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000071",
              "11000071",
              "Rudersdal Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000072",
              "11000072",
              "R\u00f8dovre Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000073",
              "11000073",
              "Sams\u00f8 Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000074",
              "11000074",
              "Silkeborg Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000075",
              "11000075",
              "Skanderborg Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000076",
              "11000076",
              "Skive Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000077",
              "11000077",
              "Slagelse Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000078",
              "11000078",
              "Solr\u00f8d Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000079",
              "11000079",
              "Sor\u00f8 Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000080",
              "11000080",
              "Stevns Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000081",
              "11000081",
              "Struer Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000082",
              "11000082",
              "Svendborg Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000083",
              "11000083",
              "Syddjurs Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000084",
              "11000084",
              "S\u00f8nderborg Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000085",
              "11000085",
              "Thisted Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000086",
              "11000086",
              "T\u00f8nder Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000087",
              "11000087",
              "T\u00e5rnby Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000088",
              "11000088",
              "Vallensb\u00e6k Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000089",
              "11000089",
              "Varde Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000090",
              "11000090",
              "Vejen Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000091",
              "11000091",
              "Vejle Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000092",
              "11000092",
              "Vesthimmerlands Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000093",
              "11000093",
              "Viborg Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000094",
              "11000094",
              "Vordingborg Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000095",
              "11000095",
              "\u00c6r\u00f8 Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000096",
              "11000096",
              "Aabenraa Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000097",
              "11000097",
              "Aalborg Kommune",
              OrganizationType.MUNICIPALITY),
          new OrgSeed(
              "c0010000-0000-0000-0000-000000000098",
              "11000098",
              "Aarhus Kommune",
              OrganizationType.MUNICIPALITY),
          // ── Regioner (5) ──────────────────────────────────────────────────
          new OrgSeed(
              "c0020000-0000-0000-0000-000000000001",
              "12000001",
              "Region Hovedstaden",
              OrganizationType.REGION),
          new OrgSeed(
              "c0020000-0000-0000-0000-000000000002",
              "12000002",
              "Region Midtjylland",
              OrganizationType.REGION),
          new OrgSeed(
              "c0020000-0000-0000-0000-000000000003",
              "12000003",
              "Region Nordjylland",
              OrganizationType.REGION),
          new OrgSeed(
              "c0020000-0000-0000-0000-000000000004",
              "12000004",
              "Region Sj\u00e6lland",
              OrganizationType.REGION),
          new OrgSeed(
              "c0020000-0000-0000-0000-000000000005",
              "12000005",
              "Region Syddanmark",
              OrganizationType.REGION),
          // ── Politi (13) ───────────────────────────────────────────────────
          new OrgSeed(
              "c0030000-0000-0000-0000-000000000001",
              "13000001",
              "Bornholms Politi",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0030000-0000-0000-0000-000000000002",
              "13000002",
              "Fyns Politi",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0030000-0000-0000-0000-000000000003",
              "13000003",
              "K\u00f8benhavns Politi",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0030000-0000-0000-0000-000000000004",
              "13000004",
              "K\u00f8benhavns Vestegns Politi",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0030000-0000-0000-0000-000000000005",
              "13000005",
              "Midt- og Vestjyllands Politi",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0030000-0000-0000-0000-000000000006",
              "13000006",
              "Midt- og Vestsj\u00e6llands Politi",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0030000-0000-0000-0000-000000000007",
              "13000007",
              "Nordjyllands Politi",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0030000-0000-0000-0000-000000000008",
              "13000008",
              "Nordsj\u00e6llands Politi",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0030000-0000-0000-0000-000000000009",
              "13000009",
              "Rigspolitiet",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0030000-0000-0000-0000-000000000010",
              "13000010",
              "Syd- og S\u00f8nderjyllands Politi",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0030000-0000-0000-0000-000000000011",
              "13000011",
              "Sydsj\u00e6llands og Lolland-Falsters Politi",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0030000-0000-0000-0000-000000000012",
              "13000012",
              "Syd\u00f8stjylland Politi",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0030000-0000-0000-0000-000000000013",
              "13000013",
              "\u00d8stjyllands Politi",
              OrganizationType.STATE_AGENCY),
          // ── Domstole (29) ─────────────────────────────────────────────────
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000001",
              "14000001",
              "Domstolsstyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000002",
              "14000002",
              "H\u00f8jesteret",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000003",
              "14000003",
              "K\u00f8benhavns Byret",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000004",
              "14000004",
              "Retten i Esbjerg",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000005",
              "14000005",
              "Retten i Glostrup",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000006",
              "14000006",
              "Retten i Helsing\u00f8r",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000007",
              "14000007",
              "Retten i Herning",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000008",
              "14000008",
              "Retten i Hiller\u00f8d",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000009",
              "14000009",
              "Retten i Hj\u00f8rring",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000010",
              "14000010",
              "Retten i Holb\u00e6k",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000011",
              "14000011",
              "Retten i Holstebro",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000012",
              "14000012",
              "Retten i Horsens",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000013",
              "14000013",
              "Retten i Kolding",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000014",
              "14000014",
              "Retten i Lyngby",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000015",
              "14000015",
              "Retten i Nyk\u00f8bing F",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000016",
              "14000016",
              "Retten i N\u00e6stved",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000017",
              "14000017",
              "Retten i Odense",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000018",
              "14000018",
              "Retten i Randers",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000019",
              "14000019",
              "Retten i Roskilde",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000020",
              "14000020",
              "Retten i Svendborg",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000021",
              "14000021",
              "Retten i S\u00f8nderborg",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000022",
              "14000022",
              "Retten i Viborg",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000023",
              "14000023",
              "Retten i Aalborg",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000024",
              "14000024",
              "Retten i \u00c5rhus",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000025",
              "14000025",
              "Retten p\u00e5 Bornholm",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000026",
              "14000026",
              "Retten p\u00e5 Frederiksberg",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000027",
              "14000027",
              "S\u00f8- og Handelsretten",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000028",
              "14000028",
              "Vestre Landsret",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0040000-0000-0000-0000-000000000029",
              "14000029",
              "\u00d8stre Landsret",
              OrganizationType.STATE_AGENCY),
          // ── Statslige organisationer (79) ─────────────────────────────────
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000001",
              "15000001",
              "Ankestyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000002",
              "15000002",
              "Arbejdstilsynet",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000003",
              "15000003",
              "Banedanmark",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000004",
              "15000004",
              "Beredskabsstyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000005",
              "15000005",
              "Besk\u00e6ftigelsesministeriets Departement",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000006",
              "15000006",
              "Bygningsstyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000007",
              "15000007",
              "B\u00f8rne- og Undervisningsministeriet",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000008",
              "15000008",
              "B\u00f8rner\u00e5det",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000009",
              "15000009",
              "Civilstyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000010",
              "15000010",
              "CPR-administrationen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000011",
              "15000011",
              "Danmarks f\u00e6ngsler",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000012",
              "15000012",
              "Danmarks Meteorologiske Institut",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000013",
              "15000013",
              "Danmarks Statistik",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000014",
              "15000014",
              "Dansk Dekommisionering",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000015",
              "15000015",
              "Datatilsynet",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000016",
              "15000016",
              "Den Hirschsprungske Samling",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000017",
              "15000017",
              "Det Danske Filminstitut",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000018",
              "15000018",
              "Det Etiske R\u00e5d og Den Nationale Videnskabsetiske Komit\u00e9",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000019",
              "15000019",
              "Det Kongelige Akademi",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000020",
              "15000020",
              "Det Kongelige Bibliotek",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000021",
              "15000021",
              "Det Kongelige Teater og Kapel",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000022",
              "15000022",
              "Det nationale Forskningscenter for Arbejdsmilj\u00f8",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000023",
              "15000023",
              "Digitaliseringsstyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000024",
              "15000024",
              "Energistyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000025",
              "15000025",
              "Erhvervsstyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000026",
              "15000026",
              "Familieretshuset",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000027",
              "15000027",
              "Finansministeriet",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000028",
              "15000028",
              "Finanstilsynet",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000029",
              "15000029",
              "Fiskeristyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000030",
              "15000030",
              "Forsvaret og Forsvarsministeriets Styrelser",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000031",
              "15000031",
              "Forsvarsministeriet",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000032",
              "15000032",
              "F\u00e6llessekretariatet for DCH og Frivilligr\u00e5det",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000033",
              "15000033",
              "F\u00e6rdselsstyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000034",
              "15000034",
              "F\u00f8devarestyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000035",
              "15000035",
              "Geodatastyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000036",
              "15000036",
              "Hjemrejsestyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000037",
              "15000037",
              "Indenrigs- og Sundhedsministeriet",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000038",
              "15000038",
              "Justitsministeriets Departement",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000039",
              "15000039",
              "Klimadatastyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000040",
              "15000040",
              "Klima- Energi- og Forsyningsministeriet",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000041",
              "15000041",
              "Konkurrence- og Forbrugerstyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000042",
              "15000042",
              "Kulturministeriet",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000043",
              "15000043",
              "Landbrugsstyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000044",
              "15000044",
              "L\u00e6gemiddelstyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000045",
              "15000045",
              "Medarbejder- og Kompetencestyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000046",
              "15000046",
              "Milj\u00f8styrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000047",
              "15000047",
              "Nationalmuseet",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000048",
              "15000048",
              "Naturstyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000049",
              "15000049",
              "N\u00e6vnenes Hus",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000050",
              "15000050",
              "Patent- og Varem\u00e6rkestyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000051",
              "15000051",
              "Rigsarkivet",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000052",
              "15000052",
              "R\u00e5det for Socialt Udsatte",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000053",
              "15000053",
              "Sikkerhedsstyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000054",
              "15000054",
              "Skatteministeriet",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000055",
              "15000055",
              "Slots- og Kulturstyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000056",
              "15000056",
              "Social- Bolig- og \u00c6ldreministeriet",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000057",
              "15000057",
              "Socialstyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000058",
              "15000058",
              "Statens Administration",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000059",
              "15000059",
              "Statens IT",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000060",
              "15000060",
              "Statens Museum for Kunst",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000061",
              "15000061",
              "Statens Serum Institut",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000062",
              "15000062",
              "Styrelsen for Arbejdsmarked og Rekruttering",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000063",
              "15000063",
              "Styrelsen for IT og L\u00e6ring",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000064",
              "15000064",
              "Styrelsen for patientklager",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000065",
              "15000065",
              "Styrelsen for Patientsikkerhed",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000066",
              "15000066",
              "Styrelsen for Undervisning og Kvalitet",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000067",
              "15000067",
              "Sundhedsdatastyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000068",
              "15000068",
              "Sundhedsstyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000069",
              "15000069",
              "S\u00f8fartsstyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000070",
              "15000070",
              "Tandskadeank\u00e9n\u00e6vnet",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000071",
              "15000071",
              "Trafikstyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000072",
              "15000072",
              "Uddannelses- og Forskningsministeriet",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000073",
              "15000073",
              "Uddannelses- og Forskningsstyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000074",
              "15000074",
              "Udenrigsministeriet",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000075",
              "15000075",
              "Udl\u00e6ndingestyrelsen",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000076",
              "15000076",
              "Udl\u00e6ndinge- og Integrationsministeriet",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000077",
              "15000077",
              "Vejdirektoratet",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000078",
              "15000078",
              "VIVE Det nationale Forsknings- og Analysecenter for Velf\u00e6rd",
              OrganizationType.STATE_AGENCY),
          new OrgSeed(
              "c0050000-0000-0000-0000-000000000079",
              "15000079",
              "\u00d8konomistyrelsen",
              OrganizationType.STATE_AGENCY),
          // ── Skatteforvaltningen (1) ───────────────────────────────────────
          new OrgSeed(
              "c0060000-0000-0000-0000-000000000001",
              "16000001",
              "Skatteforvaltningen",
              OrganizationType.STATE_AGENCY));
  // @formatter:on
}
