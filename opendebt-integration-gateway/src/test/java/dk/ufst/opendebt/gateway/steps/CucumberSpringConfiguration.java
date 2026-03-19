package dk.ufst.opendebt.gateway.steps;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import io.cucumber.spring.CucumberContextConfiguration;

@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
public class CucumberSpringConfiguration {}
