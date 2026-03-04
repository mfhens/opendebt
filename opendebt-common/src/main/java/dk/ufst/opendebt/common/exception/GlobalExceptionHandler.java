package dk.ufst.opendebt.common.exception;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dk.ufst.opendebt.common.dto.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(OpenDebtException.class)
  public ResponseEntity<ErrorResponse> handleOpenDebtException(OpenDebtException ex) {
    log.error("OpenDebt exception: {} - {}", ex.getErrorCode(), ex.getMessage(), ex);

    ErrorResponse response =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .severity(ex.getSeverity().name())
            .build();

    HttpStatus status =
        switch (ex.getSeverity()) {
          case WARNING -> HttpStatus.BAD_REQUEST;
          case ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
          case CRITICAL -> HttpStatus.SERVICE_UNAVAILABLE;
        };

    return ResponseEntity.status(status).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex) {
    List<String> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.toList());

    ErrorResponse response =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .errorCode("VALIDATION_ERROR")
            .message("Validation failed")
            .details(errors)
            .severity("WARNING")
            .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
    List<String> errors =
        ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .collect(Collectors.toList());

    ErrorResponse response =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .errorCode("CONSTRAINT_VIOLATION")
            .message("Constraint violation")
            .details(errors)
            .severity("WARNING")
            .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    log.error("Unexpected exception", ex);

    ErrorResponse response =
        ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .errorCode("INTERNAL_ERROR")
            .message("An unexpected error occurred")
            .severity("ERROR")
            .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}
