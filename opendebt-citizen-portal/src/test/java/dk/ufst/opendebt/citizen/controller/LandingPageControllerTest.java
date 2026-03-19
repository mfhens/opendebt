package dk.ufst.opendebt.citizen.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LandingPageControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void landingPage_returns200() throws Exception {
    mockMvc.perform(get("/")).andExpect(status().isOk());
  }

  @Test
  void landingPage_containsHeading() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Overblik over din")));
  }

  @Test
  void landingPage_contains7FaqItems() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("skat-faq__item")));
  }

  @Test
  void landingPage_containsSkipLink() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("skat-skip-link")));
  }

  @Test
  void landingPage_containsMainLandmark() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("role=\"main\"")));
  }

  @Test
  void landingPage_containsLanguageSelector() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("skat-language-selector")));
  }

  @Test
  void landingPage_containsFooterAccessibilityLink() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("/was")));
  }

  @Test
  void accessibilityStatement_returns200() throws Exception {
    mockMvc.perform(get("/was")).andExpect(status().isOk());
  }

  @Test
  void landingPage_switchToEnglish() throws Exception {
    mockMvc.perform(get("/").param("lang", "en-GB")).andExpect(status().isOk());
  }
}
