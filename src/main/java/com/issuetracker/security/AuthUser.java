package com.issuetracker.security;

import com.issuetracker.domain.User;

public record AuthUser(User user) {
  public java.util.UUID id() { return user.getId(); }
  public boolean isGlobalAdmin() {
    return user.getGlobalRole() == com.issuetracker.domain.Enums.GlobalRole.admin;
  }
}
