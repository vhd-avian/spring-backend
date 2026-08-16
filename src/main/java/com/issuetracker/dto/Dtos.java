package com.issuetracker.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.issuetracker.domain.Enums;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** All request/response payloads, named after the OpenAPI schemas. */
public class Dtos {

  // ---------- Users / auth ----------
  public record UserDto(UUID id, String email, String full_name, String avatar_url,
                        String bio, String phone_number, String global_role,
                        Instant created_at, Instant updated_at) {}

  public record UserCreate(@Email @NotBlank String email,
                           @NotBlank @Size(min = 4) String password,
                           @NotBlank String full_name) {}

  public record UserUpdate(String full_name, String avatar_url, String bio,
                           String phone_number, String global_role) {}

  public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

  public record AuthResponse(String access_token, String token_type, UserDto user) {}

  public record ForgotPasswordRequest(@Email @NotBlank String email) {}

  public record ForgotPasswordResponse(String message, String new_password) {}

  // ---------- Projects ----------
  public record ProjectDto(UUID id, String name, String key, String description,
                           UUID lead_user_id, boolean is_archived,
                           Instant created_at, Instant updated_at) {}

  public record ProjectCreate(@NotBlank String name,
                              @NotBlank @Pattern(regexp = "^[A-Z]{2,10}$",
                                  message = "key must be 2-10 uppercase letters") String key,
                              String description,
                              UUID lead_user_id) {}

  public record ProjectUpdate(String name, String description, UUID lead_user_id, Boolean is_archived) {}

  public record ProjectMemberDto(UUID project_id, UUID user_id, String role, UserDto user) {}

  /** Accepts either snake_case user_id or camelCase userId (frontend compatibility). */
  public record ProjectMemberAdd(UUID user_id, UUID userId, @NotNull Enums.ProjectRole role) {
    public UUID resolvedUserId() { return user_id != null ? user_id : userId; }
  }

  public record MemberRoleUpdate(@NotNull Enums.ProjectRole role) {}

  public record MemberRemove(UUID user_id, UUID userId) {
    public UUID resolvedUserId() { return user_id != null ? user_id : userId; }
  }

  // ---------- Sprints ----------
  public record SprintDto(UUID id, UUID project_id, String name, String goal,
                          LocalDate start_date, LocalDate end_date, boolean is_active,
                          Instant created_at, Instant updated_at) {}

  public record SprintCreate(@NotBlank String name, String goal,
                             LocalDate start_date, LocalDate end_date) {}

  // ---------- Issues ----------
  public record IssueDto(UUID id, String title, String description, String type, String status,
                         String priority, Integer story_points, UUID assignee_id, UUID reporter_id,
                         UUID project_id, UUID sprint_id, UUID parent_issue_id, LocalDate due_date,
                         Instant created_at, Instant updated_at,
                         UserDto assignee, UserDto reporter) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record IssueDetailDto(UUID id, String title, String description, String type, String status,
                               String priority, Integer story_points, UUID assignee_id, UUID reporter_id,
                               UUID project_id, UUID sprint_id, UUID parent_issue_id, LocalDate due_date,
                               Instant created_at, Instant updated_at,
                               UserDto assignee, UserDto reporter,
                               List<CommentDto> comments, List<AttachmentDto> attachments) {}

  public record IssueCreate(@NotBlank String title, String description,
                            @NotNull Enums.IssueType type,
                            Enums.Priority priority,
                            @Min(0) @Max(100) Integer story_points,
                            UUID assignee_id,
                            @NotNull UUID project_id,
                            UUID sprint_id,
                            UUID parent_issue_id,
                            LocalDate due_date) {}

  public record IssueUpdate(String title, String description, Enums.IssueType type,
                            Enums.Priority priority, Integer story_points, UUID assignee_id,
                            UUID sprint_id, LocalDate due_date, Enums.IssueStatus status) {}

  public record TransitionDto(String from_status, String to_status) {}

  public record TransitionRequest(@NotNull Enums.IssueStatus to_status) {}

  // ---------- Comments / attachments ----------
  public record CommentDto(UUID id, UUID issue_id, UUID author_id, String content,
                           Instant created_at, Instant updated_at, UserDto author) {}

  public record CommentCreate(@NotBlank @Size(min = 1, max = 5000) String content) {}

  public record CommentUpdate(@NotBlank @Size(min = 1, max = 5000) String content) {}

  public record AttachmentDto(UUID id, UUID issue_id, String filename, String file_url,
                              UUID uploaded_by_id, Instant uploaded_at, UserDto uploaded_by) {}

  // ---------- Notifications ----------
  public record NotificationDto(UUID id, UUID user_id, String type, String message,
                                UUID issue_id, boolean is_read, Instant created_at) {}

  // ---------- Common ----------
  public record PageResponse<T>(List<T> items, long total, int page, int limit, int total_pages) {}

  public record ErrorResponse(String error, String message, int status_code) {}
}
