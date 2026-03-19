package dk.ufst.opendebt.citizen.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class CitizenLinksModelAdvice {

  private final CitizenLinksProperties citizenLinks;

  @ModelAttribute("citizenLinks")
  public CitizenLinksProperties citizenLinks() {
    return citizenLinks;
  }
}
