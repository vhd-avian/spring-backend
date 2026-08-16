package com.issuetracker.service;

import com.issuetracker.domain.*;
import com.issuetracker.dto.Dtos.*;
import com.issuetracker.exception.ApiException;
import com.issuetracker.repo.UserRepository;
import com.issuetracker.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

@Service
public class AuthService {

  private static final String PWD_ALPHABET =
      "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789@#$%";
  private static final SecureRandom RANDOM = new SecureRandom();

  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final JwtService jwt;
  private final List<String> allowedDomains;

  public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt,
                     @Value("${app.allowed-email-domains:}") String domains) {
    this.users = users;
    this.encoder = encoder;
    this.jwt = jwt;
    this.allowedDomains = domains == null || domains.isBlank()
        ? List.of()
        : Arrays.stream(domains.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }

  /** Rule: unique email, password >= 6 chars, bcrypt hash, optional domain whitelist. */
  public UserDto register(UserCreate body) {
    String email = body.email().trim().toLowerCase();
    if (users.existsByEmailIgnoreCase(email))
      throw ApiException.badRequest("Email already registered, please login");
    if (!allowedDomains.isEmpty()) {
      String domain = email.substring(email.indexOf('@') + 1);
      if (allowedDomains.stream().noneMatch(d -> d.equalsIgnoreCase(domain)))
        throw ApiException.badRequest("Email domain not allowed");
    }
    User u = new User();
    u.setEmail(email);
    u.setFullName(body.full_name().trim());
    u.setPasswordHash(encoder.encode(body.password()));
    u.setGlobalRole(users.count() == 0 ? Enums.GlobalRole.admin : Enums.GlobalRole.user);
    return Mapper.user(users.save(u));
  }

  public AuthResponse login(LoginRequest body) {
    User u = users.findByEmailIgnoreCase(body.email().trim())
        .orElseThrow(() -> ApiException.unauthorized("Invalid credentials"));
    if (!encoder.matches(body.password(), u.getPasswordHash()))
      throw ApiException.unauthorized("Invalid credentials");
    String token = jwt.generate(u.getId().toString(), u.getEmail(), u.getGlobalRole().name());
    return new AuthResponse(token, "Bearer", Mapper.user(u));
  }

  /** Demo flow: generates a new password and returns it in the response. */
  public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest body) {
    User u = users.findByEmailIgnoreCase(body.email().trim())
        .orElseThrow(() -> ApiException.notFound("Email not found"));
    String newPassword = randomPassword();
    u.setPasswordHash(encoder.encode(newPassword));
    users.save(u);
    return new ForgotPasswordResponse("New password generated (demo)", newPassword);
  }

  private String randomPassword() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 12; i++) sb.append(PWD_ALPHABET.charAt(RANDOM.nextInt(PWD_ALPHABET.length())));
    return sb.toString();
  }
}
