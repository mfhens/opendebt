package dk.ufst.opendebt.personregistry.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.ufst.opendebt.personregistry.dto.OrganizationDto;
import dk.ufst.opendebt.personregistry.dto.OrganizationLookupRequest;
import dk.ufst.opendebt.personregistry.dto.OrganizationLookupResponse;
import dk.ufst.opendebt.personregistry.entity.OrganizationEntity.OrganizationType;
import dk.ufst.opendebt.personregistry.service.OrganizationService;

@WebMvcTest(OrganizationController.class)
class OrganizationControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private OrganizationService organizationService;

  private static final UUID ORG_ID = UUID.randomUUID();

  @Test
  @WithMockUser(roles = "SERVICE")
  void lookupOrCreate_validRequest_returnsOrganizationId() throws Exception {
    OrganizationLookupRequest request =
        OrganizationLookupRequest.builder()
            .cvr("12345678")
            .name("Test Municipality")
            .organizationType(OrganizationType.MUNICIPALITY)
            .build();

    OrganizationLookupResponse response =
        OrganizationLookupResponse.builder().organizationId(ORG_ID).build();

    when(organizationService.lookupOrCreate(any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/organizations/lookup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.organizationId").value(ORG_ID.toString()));
  }

  @Test
  @WithMockUser(roles = "SERVICE")
  void lookupOrCreate_invalidCvr_returnsBadRequest() throws Exception {
    OrganizationLookupRequest request =
        OrganizationLookupRequest.builder().cvr("invalid").name("Test").build();

    mockMvc
        .perform(
            post("/api/v1/organizations/lookup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "CASEWORKER")
  void getOrganization_validId_returnsOrganization() throws Exception {
    OrganizationDto dto =
        OrganizationDto.builder()
            .organizationId(ORG_ID)
            .cvr("12345678")
            .name("Test Municipality")
            .organizationType(OrganizationType.MUNICIPALITY)
            .active(true)
            .build();

    when(organizationService.getOrganizationById(ORG_ID)).thenReturn(dto);

    mockMvc
        .perform(get("/api/v1/organizations/{id}", ORG_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.organizationId").value(ORG_ID.toString()))
        .andExpect(jsonPath("$.cvr").value("12345678"))
        .andExpect(jsonPath("$.name").value("Test Municipality"));
  }

  @Test
  @WithMockUser(roles = "SERVICE")
  void exists_organizationExists_returnsTrue() throws Exception {
    when(organizationService.exists(ORG_ID)).thenReturn(true);

    mockMvc
        .perform(get("/api/v1/organizations/{id}/exists", ORG_ID))
        .andExpect(status().isOk())
        .andExpect(content().string("true"));
  }

  @Test
  void lookupOrCreate_unauthorized_returnsForbidden() throws Exception {
    OrganizationLookupRequest request = OrganizationLookupRequest.builder().cvr("12345678").build();

    mockMvc
        .perform(
            post("/api/v1/organizations/lookup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }
}
