package com.issuetracker.web;

import com.issuetracker.dto.Dtos.*;
import com.issuetracker.security.AuthUser;
import com.issuetracker.security.CurrentUser;
import com.issuetracker.service.AuthService;
import com.issuetracker.service.Mapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService auth;

  public AuthController(AuthService auth) { this.auth = auth; }

  @PostMapping("/register")
  public ResponseEntity<UserDto> register(@Valid @RequestBody UserCreate body) {
    return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(body));
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest body) { return auth.login(body); }

  @GetMapping("/me")
  public UserDto me(@CurrentUser AuthUser me) { return Mapper.user(me.user()); }

  @PostMapping("/forgot-password")
  public ForgotPasswordResponse forgot(@Valid @RequestBody ForgotPasswordRequest body) {
    return auth.forgotPassword(body);
  }
}
