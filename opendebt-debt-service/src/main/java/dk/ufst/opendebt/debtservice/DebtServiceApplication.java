package dk.ufst.opendebt.debtservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
    scanBasePackages = {"dk.ufst.opendebt.debtservice", "dk.ufst.opendebt.common"})
@EnableScheduling
public class DebtServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(DebtServiceApplication.class, args);
  }
}
