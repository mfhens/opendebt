package dk.ufst.opendebt.caseworker.steps;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import dk.ufst.opendebt.caseworker.client.PaymentServiceClient;

import io.cucumber.spring.CucumberContextConfiguration;

/**
 * Cucumber Spring context configuration for opendebt-caseworker-portal BDD tests.
 *
 * <p>Bootstraps the Spring application context for all step definition classes under {@code
 * dk.ufst.opendebt.caseworker.steps}. Follows the same pattern as the creditor-portal module.
 *
 * <p>Referenced by: {@code Petition057PortalSteps}
 */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

  /** Mocked payment-service client — configured per-scenario in Petition057PortalSteps. */
  @MockBean public PaymentServiceClient paymentServiceClient;
}
