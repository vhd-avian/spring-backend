package com.issuetracker.service;

import com.issuetracker.domain.*;
import com.issuetracker.dto.Dtos.*;
import com.issuetracker.exception.ApiException;
import com.issuetracker.repo.*;
import com.issuetracker.security.AuthUser;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class IssueService {

  /** Workflow: backlog->todo->in_progress->in_review->done, and done->backlog (reopen). */
  private static final Map<Enums.IssueStatus, List<Enums.IssueStatus>> WORKFLOW = Map.of(
      Enums.IssueStatus.backlog, List.of(Enums.IssueStatus.todo),
      Enums.IssueStatus.todo, List.of(Enums.IssueStatus.in_progress),
      Enums.IssueStatus.in_progress, List.of(Enums.IssueStatus.in_review),
      Enums.IssueStatus.in_review, List.of(Enums.IssueStatus.done),
      Enums.IssueStatus.done, List.of(Enums.IssueStatus.backlog)
  );

  private final IssueRepository issues;
  private final CommentRepository comments;
  private final AttachmentRepository attachments;
  private final SprintRepository sprints;
  private final UserRepository users;
  private final AccessService access;
  private final NotificationService notifications;

  public IssueService(IssueRepository issues, CommentRepository comments, AttachmentRepository attachments,
                      SprintRepository sprints, UserRepository users, AccessService access,
                      NotificationService notifications) {
    this.issues = issues; this.comments = comments; this.attachments = attachments;
    this.sprints = sprints; this.users = users; this.access = access; this.notifications = notifications;
  }

  public record IssueFilter(UUID projectId, UUID assigneeId, Enums.IssueStatus status, Enums.Priority priority,
                            Enums.IssueType type, UUID sprintId, LocalDate dueFrom, LocalDate dueTo,
                            String search) {}

  public PageResponse<IssueDto> search(IssueFilter f, int page, int limit, AuthUser me) {
    List<UUID> accessible = access.accessibleProjectIds(me);
    if (f.projectId() != null) {
      access.requireAccess(f.projectId(), me);
      accessible = List.of(f.projectId());
    }
    if (accessible.isEmpty()) return new PageResponse<>(List.of(), 0, page, limit, 0);

    final List<UUID> projectIds = accessible;
    Specification<Issue> spec = (root, query, cb) -> {
      List<Predicate> ps = new ArrayList<>();
      ps.add(root.get("projectId").in(projectIds));
      if (f.assigneeId() != null) ps.add(cb.equal(root.get("assigneeId"), f.assigneeId()));
      if (f.status() != null) ps.add(cb.equal(root.get("status"), f.status()));
      if (f.priority() != null) ps.add(cb.equal(root.get("priority"), f.priority()));
      if (f.type() != null) ps.add(cb.equal(root.get("type"), f.type()));
      if (f.sprintId() != null) ps.add(cb.equal(root.get("sprintId"), f.sprintId()));
      if (f.dueFrom() != null) ps.add(cb.greaterThanOrEqualTo(root.get("dueDate"), f.dueFrom()));
      if (f.dueTo() != null) ps.add(cb.lessThanOrEqualTo(root.get("dueDate"), f.dueTo()));
      if (f.search() != null && !f.search().isBlank()) {
        String like = "%" + f.search().toLowerCase() + "%";
        ps.add(cb.or(cb.like(cb.lower(root.get("title")), like),
                     cb.like(cb.lower(root.get("description")), like)));
      }
      return cb.and(ps.toArray(new Predicate[0]));
    };

    int p = Math.max(page, 1), l = Math.min(Math.max(limit, 1), 100);
    Page<Issue> result = issues.findAll(spec,
        PageRequest.of(p - 1, l, Sort.by(Sort.Direction.DESC, "createdAt")));
    return new PageResponse<>(result.getContent().stream().map(this::dto).toList(),
        result.getTotalElements(), p, l, result.getTotalPages());
  }

  @Transactional
  public IssueDto create(IssueCreate body, AuthUser me) {
    access.requireWrite(body.project_id(), me);

    Issue i = new Issue();
    i.setProjectId(body.project_id());
    i.setTitle(body.title());
    i.setDescription(body.description());
    i.setType(body.type());
    i.setPriority(body.priority() != null ? body.priority() : Enums.Priority.medium);
    i.setStatus(Enums.IssueStatus.backlog);
    i.setStoryPoints(body.story_points());
    i.setDueDate(body.due_date());
    i.setReporterId(me.id()); // reporter always = current user

    if (body.assignee_id() != null) {
      requireAssignableMember(body.project_id(), body.assignee_id());
      i.setAssigneeId(body.assignee_id());
    }
    if (body.parent_issue_id() != null) {
      Issue parent = issueOrThrow(body.parent_issue_id());
      if (!parent.getProjectId().equals(body.project_id()))
        throw ApiException.badRequest("Parent issue must belong to the same project");
      if (parent.getParentIssueId() != null)
        throw ApiException.badRequest("Parent issue cannot itself be a subtask");
      i.setParentIssueId(parent.getId());
    }
    if (body.sprint_id() != null) {
      Sprint s = sprints.findById(body.sprint_id())
          .orElseThrow(() -> ApiException.badRequest("Sprint not found"));
      if (!s.getProjectId().equals(body.project_id()))
        throw ApiException.badRequest("Sprint belongs to another project");
      if (!s.isActive()) throw ApiException.badRequest("Issues can only be added to an active sprint");
      i.setSprintId(s.getId());
    }
    issues.save(i);

    if (i.getAssigneeId() != null && !i.getAssigneeId().equals(me.id()))
      notifications.notify(i.getAssigneeId(), "issue_assigned",
          "You were assigned to issue: " + i.getTitle(), i.getId());
    return dto(i);
  }

  public IssueDetailDto getDetail(UUID issueId, AuthUser me) {
    Issue i = issueOrThrow(issueId);
    access.requireAccess(i.getProjectId(), me);
    List<CommentDto> c = comments.findByIssueIdOrderByCreatedAtAsc(issueId).stream()
        .map(x -> Mapper.comment(x, users.findById(x.getAuthorId()).orElse(null))).toList();
    List<AttachmentDto> a = attachments.findByIssueId(issueId).stream()
        .map(x -> Mapper.attachment(x, users.findById(x.getUploadedById()).orElse(null))).toList();
    return Mapper.issueDetail(i, userOrNull(i.getAssigneeId()), userOrNull(i.getReporterId()), c, a);
  }

  /** Editable by assignee, reporter, project lead or admin. */
  @Transactional
  public IssueDto update(UUID issueId, IssueUpdate body, AuthUser me) {
    Issue i = issueOrThrow(issueId);
    requireEditPermission(i, me);

    if (body.title() != null) i.setTitle(body.title());
    if (body.description() != null) i.setDescription(body.description());
    if (body.type() != null) i.setType(body.type());
    if (body.priority() != null) i.setPriority(body.priority());
    if (body.story_points() != null) {
      if (body.story_points() < 0 || body.story_points() > 100)
        throw ApiException.badRequest("story_points must be between 0 and 100");
      i.setStoryPoints(body.story_points());
    }
    if (body.due_date() != null) i.setDueDate(body.due_date());
    if (body.assignee_id() != null) {
      requireAssignableMember(i.getProjectId(), body.assignee_id());
      boolean changed = !body.assignee_id().equals(i.getAssigneeId());
      i.setAssigneeId(body.assignee_id());
      if (changed && !body.assignee_id().equals(me.id()))
        notifications.notify(body.assignee_id(), "issue_assigned",
            "You were assigned to issue: " + i.getTitle(), i.getId());
    }
    if (body.sprint_id() != null) {
      Sprint s = sprints.findById(body.sprint_id())
          .orElseThrow(() -> ApiException.badRequest("Sprint not found"));
      if (!s.getProjectId().equals(i.getProjectId()))
        throw ApiException.badRequest("Sprint belongs to another project");
      i.setSprintId(s.getId());
    }
    if (body.status() != null && body.status() != i.getStatus()) {
      applyTransition(i, body.status(), me);
    }
    i.setUpdatedAt(Instant.now());
    return dto(issues.save(i));
  }

  @Transactional
  public void delete(UUID issueId, AuthUser me) { delete(issueId, false, me); }

  /** cascade=false -> 409 when the issue has subtasks; cascade=true -> subtasks deleted too. */
  @Transactional
  public void delete(UUID issueId, boolean cascade, AuthUser me) {
    Issue i = issueOrThrow(issueId);
    if (!access.isLeadOrAdmin(i.getProjectId(), me))
      throw ApiException.forbidden("Only a project lead or admin can delete issues");
    List<Issue> subtasks = issues.findByParentIssueId(issueId);
    if (!subtasks.isEmpty()) {
      if (!cascade) throw ApiException.conflict("Issue has subtasks");
      for (Issue sub : subtasks) deleteSingle(sub);
    }
    deleteSingle(i);
  }

  private void deleteSingle(Issue i) {
    comments.deleteByIssueId(i.getId());
    attachments.deleteByIssueId(i.getId());
    issues.delete(i);
  }

  public List<TransitionDto> transitions(UUID issueId, AuthUser me) {
    Issue i = issueOrThrow(issueId);
    access.requireAccess(i.getProjectId(), me);
    if (!canTransition(i, me)) return List.of();
    return WORKFLOW.getOrDefault(i.getStatus(), List.of()).stream()
        .map(to -> new TransitionDto(i.getStatus().name(), to.name()))
        .toList();
  }

  @Transactional
  public IssueDto transition(UUID issueId, Enums.IssueStatus to, AuthUser me) {
    Issue i = issueOrThrow(issueId);
    applyTransition(i, to, me);
    i.setUpdatedAt(Instant.now());
    return dto(issues.save(i));
  }

  private void applyTransition(Issue i, Enums.IssueStatus to, AuthUser me) {
    if (!canTransition(i, me))
      throw ApiException.forbidden("Only the assignee, reporter, project lead or admin can transition this issue");
    if (!WORKFLOW.getOrDefault(i.getStatus(), List.of()).contains(to))
      throw ApiException.badRequest("Transition " + i.getStatus() + " -> " + to + " is not allowed");

    Enums.IssueStatus from = i.getStatus();
    i.setStatus(to);
    String msg = "Issue \"" + i.getTitle() + "\" moved from " + from + " to " + to;
    if (i.getAssigneeId() != null && !i.getAssigneeId().equals(me.id()))
      notifications.notify(i.getAssigneeId(), "status_changed", msg, i.getId());
    if (!i.getReporterId().equals(me.id()) && !i.getReporterId().equals(i.getAssigneeId()))
      notifications.notify(i.getReporterId(), "status_changed", msg, i.getId());
  }

  private boolean canTransition(Issue i, AuthUser me) {
    return me.isGlobalAdmin()
        || me.id().equals(i.getAssigneeId())
        || me.id().equals(i.getReporterId())
        || access.isLeadOrAdmin(i.getProjectId(), me);
  }

  private void requireEditPermission(Issue i, AuthUser me) {
    access.requireWrite(i.getProjectId(), me);
    boolean allowed = me.isGlobalAdmin()
        || me.id().equals(i.getAssigneeId())
        || me.id().equals(i.getReporterId())
        || access.isLeadOrAdmin(i.getProjectId(), me);
    if (!allowed) throw ApiException.forbidden("Only the assignee, reporter, project lead or admin can edit");
  }

  private void requireAssignableMember(UUID projectId, UUID userId) {
    if (!users.existsById(userId)) throw ApiException.badRequest("Assignee not found");
    if (!access.isMember(projectId, userId))
      throw ApiException.badRequest("Assignee must be a member of the project");
  }

  /** Issue payload enriched with assignee/reporter user objects. */
  private IssueDto dto(Issue i) {
    return Mapper.issue(i, userOrNull(i.getAssigneeId()), userOrNull(i.getReporterId()));
  }

  private User userOrNull(UUID id) {
    return id == null ? null : users.findById(id).orElse(null);
  }

  public Issue issueOrThrow(UUID issueId) {
    return issues.findById(issueId).orElseThrow(() -> ApiException.notFound("Issue not found"));
  }
}
