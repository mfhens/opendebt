package dk.ufst.opendebt.creditor.client;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

/**
 * Lightweight page DTO for deserializing Spring Page responses from backend services. Avoids
 * pulling in spring-data-commons as a dependency in the portal BFF.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestPage<T> {

  private final List<T> content;
  private final int number;
  private final int size;
  private final long totalElements;
  private final int totalPages;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public RestPage(
      @JsonProperty("content") List<T> content,
      @JsonProperty("number") int number,
      @JsonProperty("size") int size,
      @JsonProperty("totalElements") long totalElements,
      @JsonProperty("totalPages") int totalPages) {
    this.content = content != null ? content : new ArrayList<>();
    this.number = number;
    this.size = size > 0 ? size : 20;
    this.totalElements = totalElements;
    this.totalPages = totalPages;
  }
}
