package com.issuetracker.security;

import com.issuetracker.domain.User;
import com.issuetracker.repo.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserRepository users;

  public JwtAuthFilter(JwtService jwtService, UserRepository users) {
    this.jwtService = jwtService;
    this.users = users;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      try {
        UUID userId = UUID.fromString(jwtService.extractUserId(header.substring(7)));
        Optional<User> user = users.findById(userId);
        if (user.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
          var principal = new AuthUser(user.get());
          var auth = new UsernamePasswordAuthenticationToken(principal, null,
              List.of(new SimpleGrantedAuthority("ROLE_" + user.get().getGlobalRole().name().toUpperCase())));
          auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      } catch (Exception ignored) {
        // invalid token -> stays anonymous -> 401 from entry point
      }
    }
    chain.doFilter(request, response);
  }
}
