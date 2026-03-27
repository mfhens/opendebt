package dk.ufst.opendebt.personregistry.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import dk.ufst.opendebt.personregistry.service.PersonService;
import dk.ufst.opendebt.personregistry.service.impl.PersonServiceImpl;

// Ref: tb021-person-registry-client-integration.md §0.4
class PersonRegistryExceptionHandlerTest {

  private MockMvc mockMvc;
  private PersonService personService;

  @BeforeEach
  void setUp() {
    personService = mock(PersonService.class);
    PersonController personController = new PersonController(personService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(personController)
            .setControllerAdvice(new PersonRegistryExceptionHandler())
            .build();
  }

  // Ref: §0.3 AC-EH-1 — person not found maps to 404
  @Test
  void getPerson_whenPersonNotFound_returns404WithErrorBody() throws Exception {
    UUID personId = UUID.randomUUID();
    when(personService.getPersonById(any(UUID.class)))
        .thenThrow(new PersonServiceImpl.PersonNotFoundException("Person not found: " + personId));

    mockMvc
        .perform(get("/api/v1/persons/{id}", personId))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").value("Person not found"));
  }

  // Ref: §0.3 AC-EH-2 — soft-deleted person maps to 404
  @Test
  void getPerson_whenSoftDeletedPerson_returns404WithErrorBody() throws Exception {
    UUID personId = UUID.randomUUID();
    when(personService.getPersonById(any(UUID.class)))
        .thenThrow(new PersonServiceImpl.PersonNotFoundException("Person is deleted: " + personId));

    mockMvc
        .perform(get("/api/v1/persons/{id}", personId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Person not found"));
  }
}
