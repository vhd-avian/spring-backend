package com.issuetracker.web;

import com.issuetracker.domain.Enums;
import com.issuetracker.dto.Dtos.*;
import com.issuetracker.security.AuthUser;
import com.issuetracker.security.CurrentUser;
import com.issuetracker.service.IssueService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Issues")
@RestController
@RequestMapping("/api/v1/issues")
public class IssueController {

  private final IssueService issues;

  public IssueController(IssueService issues) { this.issues = issues; }

  @GetMapping
  public PageResponse<IssueDto> search(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(name = "project_id", required = false) UUID projectId,
      @RequestParam(name = "assignee_id", required = false) UUID assigneeId,
      @RequestParam(required = false) Enums.IssueStatus status,
      @RequestParam(required = false) Enums.Priority priority,
      @RequestParam(required = false) Enums.IssueType type,
      @RequestParam(name = "sprint_id", required = false) UUID sprintId,
      @RequestParam(name = "due_date_from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
      @RequestParam(name = "due_date_to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
      @RequestParam(required = false) String search,
      @CurrentUser AuthUser me) {
    var filter = new IssueService.IssueFilter(projectId, assigneeId, status, priority, type, sprintId,
        dueFrom, dueTo, search);
    return issues.search(filter, page, limit, me);
  }

  @PostMapping
  public ResponseEntity<IssueDto> create(@Valid @RequestBody IssueCreate body, @CurrentUser AuthUser me) {
    return ResponseEntity.status(HttpStatus.CREATED).body(issues.create(body, me));
  }

  @GetMapping("/{issueId}")
  public IssueDetailDto get(@PathVariable UUID issueId, @CurrentUser AuthUser me) {
    return issues.getDetail(issueId, me);
  }

  @PutMapping("/{issueId}")
  public IssueDto update(@PathVariable UUID issueId, @RequestBody IssueUpdate body, @CurrentUser AuthUser me) {
    return issues.update(issueId, body, me);
  }

  @DeleteMapping("/{issueId}")
  public ResponseEntity<Void> delete(@PathVariable UUID issueId,
                                     @RequestParam(defaultValue = "false") boolean cascade,
                                     @CurrentUser AuthUser me) {
    issues.delete(issueId, cascade, me);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{issueId}/transitions")
  public List<TransitionDto> transitions(@PathVariable UUID issueId, @CurrentUser AuthUser me) {
    return issues.transitions(issueId, me);
  }

  @PostMapping("/{issueId}/transitions")
  public IssueDto transition(@PathVariable UUID issueId, @Valid @RequestBody TransitionRequest body,
                             @CurrentUser AuthUser me) {
    return issues.transition(issueId, body.to_status(), me);
  }
}
