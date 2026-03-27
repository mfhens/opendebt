package dk.ufst.opendebt.personregistry.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dk.ufst.opendebt.personregistry.service.impl.PersonServiceImpl;

@RestControllerAdvice
public class PersonRegistryExceptionHandler {

  @ExceptionHandler(PersonServiceImpl.PersonNotFoundException.class)
  public ResponseEntity<Map<String, String>> handlePersonNotFound(
      PersonServiceImpl.PersonNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Person not found"));
  }
}
