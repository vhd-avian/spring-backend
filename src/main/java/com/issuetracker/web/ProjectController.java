package com.issuetracker.web;

import com.issuetracker.dto.Dtos.*;
import com.issuetracker.security.AuthUser;
import com.issuetracker.security.CurrentUser;
import com.issuetracker.service.ProjectService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Projects")
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

  private final ProjectService projects;

  public ProjectController(ProjectService projects) { this.projects = projects; }

  @GetMapping
  public PageResponse<ProjectDto> list(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int limit,
                                       @RequestParam(name = "include_archived", defaultValue = "false") boolean includeArchived,
                                       @CurrentUser AuthUser me) {
    return projects.list(me, page, limit, includeArchived);
  }

  @PostMapping
  public ResponseEntity<ProjectDto> create(@Valid @RequestBody ProjectCreate body, @CurrentUser AuthUser me) {
    return ResponseEntity.status(HttpStatus.CREATED).body(projects.create(body, me));
  }

  @GetMapping("/{projectId}")
  public ProjectDto get(@PathVariable UUID projectId, @CurrentUser AuthUser me) {
    return projects.get(projectId, me);
  }

  @PutMapping("/{projectId}")
  public ProjectDto update(@PathVariable UUID projectId, @RequestBody ProjectUpdate body,
                           @CurrentUser AuthUser me) {
    return projects.update(projectId, body, me);
  }

  @DeleteMapping("/{projectId}")
  public ResponseEntity<Void> delete(@PathVariable UUID projectId, @CurrentUser AuthUser me) {
    projects.delete(projectId, me);
    return ResponseEntity.noContent().build();
  }

  // ----- members -----

  @GetMapping("/{projectId}/members")
  public List<ProjectMemberDto> members(@PathVariable UUID projectId, @CurrentUser AuthUser me) {
    return projects.listMembers(projectId, me);
  }

  @PostMapping("/{projectId}/members")
  public ResponseEntity<ProjectMemberDto> addMember(@PathVariable UUID projectId,
                                                    @Valid @RequestBody ProjectMemberAdd body,
                                                    @CurrentUser AuthUser me) {
    return ResponseEntity.status(HttpStatus.CREATED).body(projects.addMember(projectId, body, me));
  }

  @DeleteMapping("/{projectId}/members")
  public ResponseEntity<Void> removeMember(@PathVariable UUID projectId,
                                           @RequestBody MemberRemove body,
                                           @CurrentUser AuthUser me) {
    if (body.resolvedUserId() == null)
      throw com.issuetracker.exception.ApiException.badRequest("user_id is required");
    projects.removeMember(projectId, body.resolvedUserId(), me);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{projectId}/members/{userId}")
  public ProjectMemberDto updateMemberRole(@PathVariable UUID projectId, @PathVariable UUID userId,
                                           @Valid @RequestBody MemberRoleUpdate body,
                                           @CurrentUser AuthUser me) {
    return projects.updateMemberRole(projectId, userId, body.role(), me);
  }
}
