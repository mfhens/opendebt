package dk.ufst.opendebt.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

// AIDEV-NOTE: @EnableAsync required for ImmuLedgerClient.appendAsync (TB-028, ADR-0029).
// AIDEV-NOTE: @EnableRetry required for @Retryable on ImmuLedgerClient.appendAsync (TB-028).
// AIDEV-TODO: @Async + @Retryable ordering caveat — see ImmuLedgerClient Javadoc.
@SpringBootApplication
@EnableAsync
@EnableRetry
public class PaymentServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(PaymentServiceApplication.class, args);
  }
}
