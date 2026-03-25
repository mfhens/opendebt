package dk.ufst.opendebt.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    scanBasePackages = {
      "dk.ufst.opendebt.gateway",
      "dk.ufst.opendebt.common.soap",
      "dk.ufst.opendebt.common.audit.cls"
    })
public class IntegrationGatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(IntegrationGatewayApplication.class, args);
  }
}
