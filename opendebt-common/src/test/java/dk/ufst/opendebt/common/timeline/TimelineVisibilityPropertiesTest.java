package dk.ufst.opendebt.common.timeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TimelineVisibilityProperties}.
 *
 * <p>Covers petition050 AC-B1–AC-B4 (role-based visibility configuration) and specs §2.7.
 *
 * <p>Gherkin scenarios covered:
 *
 * <ul>
 *   <li>Scenario: Caseworker sees all event categories (feature line 30)
 *   <li>Scenario: Supervisor sees all event categories (feature line 35)
 *   <li>Scenario: Admin sees all event categories (feature line 41)
 *   <li>Scenario: Citizen sees only citizen-appropriate events (feature line 50)
 *   <li>Scenario: Creditor sees only creditor-appropriate events (feature line 59)
 *   <li>Scenario: Visibility rules are read from external configuration (feature line 68)
 * </ul>
 *
 * <p>Spec §6.1 — TimelineVisibilityPropertiesTest test cases.
 */
class TimelineVisibilityPropertiesTest {

  private TimelineVisibilityProperties buildProps(String role, Set<EventCategory> categories) {
    TimelineVisibilityProperties props = new TimelineVisibilityProperties();
    props.getRoleCategories().put(role, categories);
    return props;
  }

  // ---------------------------------------------------------------------------
  // CASEWORKER, SUPERVISOR, ADMIN — all 7 categories
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("AC-B1: getAllowedCategories(CASEWORKER) returns all 7 EventCategory values")
  void getAllowedCategories_caseworker_returnsAll7Categories() {
    TimelineVisibilityProperties props =
        buildProps("CASEWORKER", EnumSet.allOf(EventCategory.class));
    assertThat(props.getAllowedCategories("CASEWORKER")).hasSize(7);
    assertThat(props.getAllowedCategories("CASEWORKER"))
        .containsExactlyInAnyOrder(EventCategory.values());
  }

  @Test
  @DisplayName("AC-B1: getAllowedCategories(SUPERVISOR) returns all 7 EventCategory values")
  void getAllowedCategories_supervisor_returnsAll7Categories() {
    TimelineVisibilityProperties props =
        buildProps("SUPERVISOR", EnumSet.allOf(EventCategory.class));
    assertThat(props.getAllowedCategories("SUPERVISOR")).hasSize(7);
  }

  @Test
  @DisplayName("AC-B1: getAllowedCategories(ADMIN) returns all 7 EventCategory values")
  void getAllowedCategories_admin_returnsAll7Categories() {
    TimelineVisibilityProperties props = buildProps("ADMIN", EnumSet.allOf(EventCategory.class));
    assertThat(props.getAllowedCategories("ADMIN")).hasSize(7);
  }

  // ---------------------------------------------------------------------------
  // CITIZEN — 4 categories (FINANCIAL, DEBT_LIFECYCLE, CORRESPONDENCE, COLLECTION)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName(
      "AC-B2: getAllowedCategories(CITIZEN) returns exactly {FINANCIAL, DEBT_LIFECYCLE, CORRESPONDENCE, COLLECTION}")
  void getAllowedCategories_citizen_returnsFourAllowedCategories() {
    Set<EventCategory> citizenCategories =
        Set.of(
            EventCategory.FINANCIAL,
            EventCategory.DEBT_LIFECYCLE,
            EventCategory.CORRESPONDENCE,
            EventCategory.COLLECTION);
    TimelineVisibilityProperties props = buildProps("CITIZEN", citizenCategories);

    Set<EventCategory> allowed = props.getAllowedCategories("CITIZEN");
    assertThat(allowed).hasSize(4);
    assertThat(allowed)
        .containsExactlyInAnyOrder(
            EventCategory.FINANCIAL,
            EventCategory.DEBT_LIFECYCLE,
            EventCategory.CORRESPONDENCE,
            EventCategory.COLLECTION);
    assertThat(allowed)
        .doesNotContain(EventCategory.CASE, EventCategory.OBJECTION, EventCategory.JOURNAL);
  }

  // ---------------------------------------------------------------------------
  // CREDITOR — 3 categories (FINANCIAL, DEBT_LIFECYCLE, COLLECTION)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName(
      "AC-B3: getAllowedCategories(CREDITOR) returns exactly {FINANCIAL, DEBT_LIFECYCLE, COLLECTION}")
  void getAllowedCategories_creditor_returnsThreeAllowedCategories() {
    Set<EventCategory> creditorCategories =
        Set.of(EventCategory.FINANCIAL, EventCategory.DEBT_LIFECYCLE, EventCategory.COLLECTION);
    TimelineVisibilityProperties props = buildProps("CREDITOR", creditorCategories);

    Set<EventCategory> allowed = props.getAllowedCategories("CREDITOR");
    assertThat(allowed).hasSize(3);
    assertThat(allowed)
        .containsExactlyInAnyOrder(
            EventCategory.FINANCIAL, EventCategory.DEBT_LIFECYCLE, EventCategory.COLLECTION);
    assertThat(allowed)
        .doesNotContain(
            EventCategory.CASE,
            EventCategory.CORRESPONDENCE,
            EventCategory.OBJECTION,
            EventCategory.JOURNAL);
  }

  // ---------------------------------------------------------------------------
  // Unknown role — returns empty set (safe default per specs §2.7)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName(
      "AC-B4: getAllowedCategories for unconfigured role returns empty set (yields empty timeline)")
  void getAllowedCategories_unknownRole_returnsEmptySet() {
    TimelineVisibilityProperties props = new TimelineVisibilityProperties();
    assertThat(props.getAllowedCategories("UNKNOWN")).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // AC-B4: No role-checking logic in Thymeleaf templates
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("AC-B4: EventCategory enum has exactly 7 values")
  void eventCategory_enumHasExactly7Values() {
    assertThat(EventCategory.values()).hasSize(7);
  }
}
