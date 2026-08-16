package com.issuetracker.service;

import com.issuetracker.domain.*;
import com.issuetracker.dto.Dtos.*;
import com.issuetracker.exception.ApiException;
import com.issuetracker.repo.UserRepository;
import com.issuetracker.security.AuthUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

  private final UserRepository users;

  public UserService(UserRepository users) { this.users = users; }

  public List<UserDto> search(String q, int limit) {
    int capped = Math.min(Math.max(limit, 1), 200);
    var page = PageRequest.of(0, capped);
    List<User> result = (q == null || q.isBlank())
        ? users.findAll(page).getContent()
        : users.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(q, q, page);
    return result.stream().map(Mapper::user).toList();
  }

  public UserDto get(UUID id) {
    return Mapper.user(users.findById(id).orElseThrow(() -> ApiException.notFound("User not found")));
  }

  /** Self can change full_name/avatar_url; only global admin can change global_role. */
  public UserDto update(UUID id, UserUpdate body, AuthUser me) {
    User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
    boolean self = me.id().equals(id);
    if (!self && !me.isGlobalAdmin()) throw ApiException.forbidden("Cannot update another user");

    if (body.full_name() != null) u.setFullName(body.full_name());
    if (body.avatar_url() != null) u.setAvatarUrl(body.avatar_url());
    if (body.bio() != null) u.setBio(body.bio());
    if (body.phone_number() != null) u.setPhoneNumber(body.phone_number());
    if (body.global_role() != null) {
      if (!me.isGlobalAdmin()) throw ApiException.forbidden("Only admins can change global_role");
      u.setGlobalRole(Enums.GlobalRole.valueOf(body.global_role()));
    }
    u.setUpdatedAt(java.time.Instant.now());
    return Mapper.user(users.save(u));
  }
}
