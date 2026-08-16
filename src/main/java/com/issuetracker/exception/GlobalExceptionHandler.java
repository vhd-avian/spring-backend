package com.issuetracker.exception;

import com.issuetracker.dto.Dtos.ErrorResponse;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
    return ResponseEntity.status(ex.getStatus())
        .body(new ErrorResponse(ex.getStatus().getReasonPhrase(), ex.getMessage(), ex.getStatus().value()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    String msg = ex.getBindingResult().getFieldErrors().stream()
        .map(e -> e.getField() + ": " + e.getDefaultMessage())
        .collect(Collectors.joining("; "));
    return ResponseEntity.badRequest().body(new ErrorResponse("Bad Request", msg, 400));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleDenied(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ErrorResponse("Forbidden", ex.getMessage(), 403));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleOther(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse("Internal Server Error", ex.getMessage(), 500));
  }
}
