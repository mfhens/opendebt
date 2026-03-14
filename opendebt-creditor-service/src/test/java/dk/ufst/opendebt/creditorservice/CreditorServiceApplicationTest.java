package dk.ufst.opendebt.creditorservice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CreditorServiceApplicationTest {

  @Autowired private ApplicationContext context;

  @Test
  void contextLoads() {
    assertThat(context).isNotNull();
  }

  @Test
  void applicationStartsSuccessfully() {
    assertThat(context.getId()).contains("creditor-service");
  }
}
