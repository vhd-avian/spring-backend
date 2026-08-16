package com.issuetracker.web;

import com.issuetracker.dto.Dtos.*;
import com.issuetracker.security.AuthUser;
import com.issuetracker.security.CurrentUser;
import com.issuetracker.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService users;

  public UserController(UserService users) { this.users = users; }

  @GetMapping
  public List<UserDto> list(@RequestParam(required = false) String q,
                            @RequestParam(defaultValue = "50") int limit) {
    return users.search(q, limit);
  }

  @GetMapping("/{userId}")
  public UserDto get(@PathVariable UUID userId) { return users.get(userId); }

  @PatchMapping("/{userId}")
  public UserDto update(@PathVariable UUID userId, @RequestBody UserUpdate body, @CurrentUser AuthUser me) {
    return users.update(userId, body, me);
  }
}
