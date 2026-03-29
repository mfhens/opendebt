package dk.ufst.opendebt.caseworker.steps;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
@ActiveProfiles("test")
public class CucumberSpringConfiguration {}
