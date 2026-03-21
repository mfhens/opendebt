package dk.ufst.opendebt.debtservice.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InterestRuleCodeTest {

  @Test
  void standardRate_hasConfigKey() {
    assertThat(InterestRuleCode.INDR_STD.getConfigKey()).isEqualTo("RATE_INDR_STD");
    assertThat(InterestRuleCode.INDR_STD.isExempt()).isFalse();
    assertThat(InterestRuleCode.INDR_STD.usesContractualRate()).isFalse();
  }

  @Test
  void toldRate_hasConfigKey() {
    assertThat(InterestRuleCode.INDR_TOLD.getConfigKey()).isEqualTo("RATE_INDR_TOLD");
  }

  @Test
  void toldAfdRate_hasConfigKey() {
    assertThat(InterestRuleCode.INDR_TOLD_AFD.getConfigKey()).isEqualTo("RATE_INDR_TOLD_AFD");
  }

  @Test
  void exemptRate_isExemptAndHasNoConfigKey() {
    assertThat(InterestRuleCode.INDR_EXEMPT.getConfigKey()).isNull();
    assertThat(InterestRuleCode.INDR_EXEMPT.isExempt()).isTrue();
    assertThat(InterestRuleCode.INDR_EXEMPT.usesContractualRate()).isFalse();
  }

  @Test
  void contractRate_usesContractualRateAndHasNoConfigKey() {
    assertThat(InterestRuleCode.INDR_CONTRACT.getConfigKey()).isNull();
    assertThat(InterestRuleCode.INDR_CONTRACT.isExempt()).isFalse();
    assertThat(InterestRuleCode.INDR_CONTRACT.usesContractualRate()).isTrue();
  }

  @Test
  void opkStd_hasConfigKey() {
    assertThat(InterestRuleCode.OPK_STD.getConfigKey()).isEqualTo("RATE_OPK_STD");
    assertThat(InterestRuleCode.OPK_STD.isExempt()).isFalse();
    assertThat(InterestRuleCode.OPK_STD.usesContractualRate()).isFalse();
  }

  @Test
  void allValues_areSixInTotal() {
    assertThat(InterestRuleCode.values()).hasSize(6);
  }
}
