package dk.ufst.opendebt.personregistry.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.ufst.opendebt.personregistry.dto.GdprExportResponse;
import dk.ufst.opendebt.personregistry.dto.PersonDto;
import dk.ufst.opendebt.personregistry.dto.PersonLookupRequest;
import dk.ufst.opendebt.personregistry.dto.PersonLookupResponse;
import dk.ufst.opendebt.personregistry.service.PersonService;

@WebMvcTest(PersonController.class)
class PersonControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private PersonService personService;

  private static final UUID PERSON_ID = UUID.randomUUID();

  @Test
  @WithMockUser(roles = "SERVICE")
  void lookupOrCreate_validRequest_returnsOk() throws Exception {
    PersonLookupRequest request =
        PersonLookupRequest.builder()
            .identifier("1234567890")
            .identifierType(PersonLookupRequest.IdentifierType.CPR)
            .role(PersonLookupRequest.PersonRole.PERSONAL)
            .name("Test Person")
            .build();

    PersonLookupResponse response =
        PersonLookupResponse.builder().personId(PERSON_ID).created(true).role("PERSONAL").build();

    when(personService.lookupOrCreate(any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/persons/lookup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.personId").value(PERSON_ID.toString()))
        .andExpect(jsonPath("$.created").value(true));
  }

  @Test
  @WithMockUser(roles = "SERVICE")
  void lookupOrCreate_invalidIdentifier_returnsBadRequest() throws Exception {
    PersonLookupRequest request =
        PersonLookupRequest.builder()
            .identifier("invalid")
            .identifierType(PersonLookupRequest.IdentifierType.CPR)
            .role(PersonLookupRequest.PersonRole.PERSONAL)
            .build();

    mockMvc
        .perform(
            post("/api/v1/persons/lookup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "CASEWORKER")
  void getPerson_validId_returnsPersonDto() throws Exception {
    PersonDto dto =
        PersonDto.builder()
            .id(PERSON_ID)
            .identifierType("CPR")
            .role("PERSONAL")
            .name("Test Person")
            .deleted(false)
            .build();

    when(personService.getPersonById(PERSON_ID)).thenReturn(dto);

    mockMvc
        .perform(get("/api/v1/persons/{id}", PERSON_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(PERSON_ID.toString()))
        .andExpect(jsonPath("$.name").value("Test Person"));
  }

  @Test
  @WithMockUser(roles = "SERVICE")
  void updatePerson_validRequest_returnsUpdatedDto() throws Exception {
    PersonDto dto =
        PersonDto.builder()
            .id(PERSON_ID)
            .identifierType("CPR")
            .role("PERSONAL")
            .name("Updated Person")
            .deleted(false)
            .build();

    when(personService.updatePerson(eq(PERSON_ID), any(PersonDto.class))).thenReturn(dto);

    mockMvc
        .perform(
            put("/api/v1/persons/{id}", PERSON_ID)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Person"));
  }

  @Test
  @WithMockUser(roles = "SERVICE")
  void exists_personExists_returnsTrue() throws Exception {
    when(personService.exists(PERSON_ID)).thenReturn(true);

    mockMvc
        .perform(get("/api/v1/persons/{id}/exists", PERSON_ID))
        .andExpect(status().isOk())
        .andExpect(content().string("true"));
  }

  @Test
  @WithMockUser(roles = "SERVICE")
  void exists_personNotExists_returnsFalse() throws Exception {
    when(personService.exists(PERSON_ID)).thenReturn(false);

    mockMvc
        .perform(get("/api/v1/persons/{id}/exists", PERSON_ID))
        .andExpect(status().isOk())
        .andExpect(content().string("false"));
  }

  @Test
  @WithMockUser(roles = "GDPR_OFFICER")
  void exportData_validId_returnsExportResponse() throws Exception {
    GdprExportResponse response =
        GdprExportResponse.builder()
            .personId(PERSON_ID)
            .exportedAt(LocalDateTime.now())
            .name("Test Person")
            .identifierType("CPR")
            .build();

    when(personService.exportPersonData(PERSON_ID)).thenReturn(response);

    mockMvc
        .perform(post("/api/v1/persons/{id}/gdpr/export", PERSON_ID).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.personId").value(PERSON_ID.toString()));
  }

  @Test
  @WithMockUser(roles = "GDPR_OFFICER")
  void requestErasure_validRequest_returnsAccepted() throws Exception {
    doNothing().when(personService).requestDeletion(PERSON_ID, "GDPR request");

    mockMvc
        .perform(
            delete("/api/v1/persons/{id}/gdpr/erase", PERSON_ID)
                .with(csrf())
                .param("reason", "GDPR request"))
        .andExpect(status().isAccepted());
  }

  @Test
  void lookupOrCreate_unauthorized_returnsUnauthorized() throws Exception {
    PersonLookupRequest request =
        PersonLookupRequest.builder()
            .identifier("1234567890")
            .identifierType(PersonLookupRequest.IdentifierType.CPR)
            .role(PersonLookupRequest.PersonRole.PERSONAL)
            .build();

    mockMvc
        .perform(
            post("/api/v1/persons/lookup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }
}
