package dk.ufst.opendebt.citizen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

@SpringBootApplication(
    exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class CitizenPortalApplication {

  public static void main(String[] args) {
    SpringApplication.run(CitizenPortalApplication.class, args);
  }
}
