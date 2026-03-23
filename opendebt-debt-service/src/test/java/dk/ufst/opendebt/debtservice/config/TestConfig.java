package dk.ufst.opendebt.debtservice.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import dk.ufst.opendebt.common.audit.cls.ClsAuditClient;
import dk.ufst.opendebt.debtservice.client.CreditorServiceClient;
import dk.ufst.opendebt.debtservice.client.ValidateActionRequest;
import dk.ufst.opendebt.debtservice.client.ValidateActionResponse;

@TestConfiguration
public class TestConfig {

  @Bean
  @Primary
  public CreditorServiceClient creditorServiceClient() {
    CreditorServiceClient mockClient = mock(CreditorServiceClient.class);
    when(mockClient.validateAction(any(UUID.class), any(ValidateActionRequest.class)))
        .thenReturn(ValidateActionResponse.builder().allowed(true).build());
    when(mockClient.isCreditorAllowedToCreateClaim(any(UUID.class))).thenReturn(true);
    when(mockClient.isCreditorAllowedToUpdateClaim(any(UUID.class))).thenReturn(true);
    return mockClient;
  }

  @Bean
  @Primary
  public ClsAuditClient clsAuditClient() {
    ClsAuditClient mockClient = mock(ClsAuditClient.class);
    when(mockClient.isEnabled()).thenReturn(false);
    return mockClient;
  }
}
