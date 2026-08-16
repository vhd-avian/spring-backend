package com.issuetracker.service;

import com.issuetracker.domain.*;
import com.issuetracker.dto.Dtos.*;
import com.issuetracker.exception.ApiException;
import com.issuetracker.repo.*;
import com.issuetracker.security.AuthUser;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class SprintService {

  private final SprintRepository sprints;
  private final IssueRepository issues;
  private final AccessService access;

  public SprintService(SprintRepository sprints, IssueRepository issues, AccessService access) {
    this.sprints = sprints; this.issues = issues; this.access = access;
  }

  public List<SprintDto> list(UUID projectId, boolean includeClosed, AuthUser me) {
    access.requireAccess(projectId, me);
    LocalDate today = LocalDate.now();
    return sprints.findByProjectId(projectId).stream()
        .filter(s -> includeClosed || s.isActive() || s.getEndDate() == null || !s.getEndDate().isBefore(today))
        .sorted(Comparator.comparing(Sprint::getCreatedAt))
        .map(Mapper::sprint)
        .toList();
  }

  /** Unique name per project, end_date >= start_date, created inactive. */
  public SprintDto create(UUID projectId, SprintCreate body, AuthUser me) {
    access.requireLead(projectId, me);
    if (sprints.existsByProjectIdAndNameIgnoreCase(projectId, body.name()))
      throw ApiException.badRequest("Sprint name must be unique per project");
    validateDates(body.start_date(), body.end_date());

    Sprint s = new Sprint();
    s.setProjectId(projectId);
    s.setName(body.name());
    s.setGoal(body.goal());
    s.setStartDate(body.start_date());
    s.setEndDate(body.end_date());
    s.setActive(false);
    return Mapper.sprint(sprints.save(s));
  }

  public SprintDto get(UUID sprintId, AuthUser me) {
    Sprint s = sprintOrThrow(sprintId);
    access.requireAccess(s.getProjectId(), me);
    return Mapper.sprint(s);
  }

  /** project_id is immutable; dates of an active sprint cannot be moved. */
  public SprintDto update(UUID sprintId, SprintCreate body, AuthUser me) {
    Sprint s = sprintOrThrow(sprintId);
    access.requireLead(s.getProjectId(), me);
    validateDates(body.start_date(), body.end_date());

    if (s.isActive() && (notEqual(s.getStartDate(), body.start_date())))
      throw ApiException.badRequest("Cannot change start_date of an active sprint");

    if (!s.getName().equalsIgnoreCase(body.name())
        && sprints.existsByProjectIdAndNameIgnoreCase(s.getProjectId(), body.name()))
      throw ApiException.badRequest("Sprint name must be unique per project");

    s.setName(body.name());
    s.setGoal(body.goal());
    if (!s.isActive()) s.setStartDate(body.start_date());
    s.setEndDate(body.end_date());
    s.setUpdatedAt(Instant.now());
    return Mapper.sprint(sprints.save(s));
  }

  public void delete(UUID sprintId, AuthUser me) {
    Sprint s = sprintOrThrow(sprintId);
    access.requireLead(s.getProjectId(), me);
    if (s.isActive()) throw ApiException.badRequest("Cannot delete an active sprint");
    if (!issues.findBySprintId(sprintId).isEmpty()) throw ApiException.conflict("Sprint contains issues");
    sprints.delete(s);
  }

  /** Activating deactivates the other active sprint of the same project. */
  @Transactional
  public SprintDto activate(UUID sprintId, AuthUser me) {
    Sprint s = sprintOrThrow(sprintId);
    access.requireLead(s.getProjectId(), me);
    if (s.isActive()) throw ApiException.badRequest("Sprint is already active");
    if (s.getStartDate() != null && s.getStartDate().isAfter(LocalDate.now()))
      throw ApiException.badRequest("Sprint start_date is in the future");

    sprints.findByProjectIdAndActiveTrue(s.getProjectId()).ifPresent(other -> {
      other.setActive(false);
      other.setUpdatedAt(Instant.now());
      sprints.save(other);
    });
    s.setActive(true);
    s.setUpdatedAt(Instant.now());
    return Mapper.sprint(sprints.save(s));
  }

  /** Completing moves unfinished issues back to the backlog. */
  @Transactional
  public SprintDto complete(UUID sprintId, AuthUser me) {
    Sprint s = sprintOrThrow(sprintId);
    access.requireLead(s.getProjectId(), me);
    if (!s.isActive()) throw ApiException.badRequest("Sprint is not active");

    for (Issue i : issues.findBySprintId(sprintId)) {
      if (i.getStatus() != Enums.IssueStatus.done) {
        i.setStatus(Enums.IssueStatus.backlog);
        i.setSprintId(null);
        i.setUpdatedAt(Instant.now());
        issues.save(i);
      }
    }
    s.setActive(false);
    s.setUpdatedAt(Instant.now());
    return Mapper.sprint(sprints.save(s));
  }

  public Sprint sprintOrThrow(UUID sprintId) {
    return sprints.findById(sprintId).orElseThrow(() -> ApiException.notFound("Sprint not found"));
  }

  private void validateDates(LocalDate start, LocalDate end) {
    if (start != null && end != null && end.isBefore(start))
      throw ApiException.badRequest("end_date must be on or after start_date");
  }

  private boolean notEqual(LocalDate a, LocalDate b) {
    return a == null ? b != null : !a.equals(b);
  }
}
