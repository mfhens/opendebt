package dk.ufst.opendebt.gateway.steps;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import dk.ufst.opendebt.gateway.TestClsAuditCapture;
import dk.ufst.opendebt.gateway.TestDataSourceConfig;
import dk.ufst.opendebt.gateway.TestSecurityConfig;

import io.cucumber.spring.CucumberContextConfiguration;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestDataSourceConfig.class, TestSecurityConfig.class, TestClsAuditCapture.class})
public class CucumberSpringConfiguration {}
