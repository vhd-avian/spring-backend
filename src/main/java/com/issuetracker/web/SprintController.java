package com.issuetracker.web;

import com.issuetracker.dto.Dtos.*;
import com.issuetracker.security.AuthUser;
import com.issuetracker.security.CurrentUser;
import com.issuetracker.service.SprintService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Sprints")
@RestController
@RequestMapping("/api/v1")
public class SprintController {

  private final SprintService sprints;

  public SprintController(SprintService sprints) { this.sprints = sprints; }

  @GetMapping("/projects/{projectId}/sprints")
  public List<SprintDto> list(@PathVariable UUID projectId,
                              @RequestParam(name = "include_closed", defaultValue = "true") boolean includeClosed,
                              @CurrentUser AuthUser me) {
    return sprints.list(projectId, includeClosed, me);
  }

  @PostMapping("/projects/{projectId}/sprints")
  public ResponseEntity<SprintDto> create(@PathVariable UUID projectId,
                                          @Valid @RequestBody SprintCreate body,
                                          @CurrentUser AuthUser me) {
    return ResponseEntity.status(HttpStatus.CREATED).body(sprints.create(projectId, body, me));
  }

  @GetMapping("/sprints/{sprintId}")
  public SprintDto get(@PathVariable UUID sprintId, @CurrentUser AuthUser me) {
    return sprints.get(sprintId, me);
  }

  @PutMapping("/sprints/{sprintId}")
  public SprintDto update(@PathVariable UUID sprintId, @Valid @RequestBody SprintCreate body,
                          @CurrentUser AuthUser me) {
    return sprints.update(sprintId, body, me);
  }

  @DeleteMapping("/sprints/{sprintId}")
  public ResponseEntity<Void> delete(@PathVariable UUID sprintId, @CurrentUser AuthUser me) {
    sprints.delete(sprintId, me);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/sprints/{sprintId}/activate")
  public SprintDto activate(@PathVariable UUID sprintId, @CurrentUser AuthUser me) {
    return sprints.activate(sprintId, me);
  }

  @PostMapping("/sprints/{sprintId}/complete")
  public SprintDto complete(@PathVariable UUID sprintId, @CurrentUser AuthUser me) {
    return sprints.complete(sprintId, me);
  }
}
