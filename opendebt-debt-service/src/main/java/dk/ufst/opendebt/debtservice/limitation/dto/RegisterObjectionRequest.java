package dk.ufst.opendebt.debtservice.limitation.dto;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterObjectionRequest {

  @JsonIgnore
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  @Default
  private Map<String, Object> unexpectedFields = new LinkedHashMap<>();

  @JsonAnySetter
  void captureUnexpectedField(String name, Object value) {
    unexpectedFields.put(name, value);
  }

  @JsonIgnore
  public boolean hasUnexpectedFields() {
    return !unexpectedFields.isEmpty();
  }
}
