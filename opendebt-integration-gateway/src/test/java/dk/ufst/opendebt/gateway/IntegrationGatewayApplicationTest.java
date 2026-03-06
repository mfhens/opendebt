package dk.ufst.opendebt.gateway;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class IntegrationGatewayApplicationTest {

  @Test
  void mainDelegatesToSpringApplication() {
    try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
      String[] args = {"--spring.main.web-application-type=none"};

      IntegrationGatewayApplication.main(args);

      springApplication.verify(
          () -> SpringApplication.run(IntegrationGatewayApplication.class, args));
    }
  }
}
