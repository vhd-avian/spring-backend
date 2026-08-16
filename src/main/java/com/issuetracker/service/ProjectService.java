package com.issuetracker.service;

import com.issuetracker.domain.*;
import com.issuetracker.dto.Dtos.*;
import com.issuetracker.exception.ApiException;
import com.issuetracker.repo.*;
import com.issuetracker.security.AuthUser;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class ProjectService {

  private final ProjectRepository projects;
  private final ProjectMemberRepository members;
  private final UserRepository users;
  private final IssueRepository issues;
  private final SprintRepository sprints;
  private final CommentRepository comments;
  private final AttachmentRepository attachments;
  private final AccessService access;

  public ProjectService(ProjectRepository projects, ProjectMemberRepository members, UserRepository users,
                        IssueRepository issues, SprintRepository sprints, CommentRepository comments,
                        AttachmentRepository attachments, AccessService access) {
    this.projects = projects; this.members = members; this.users = users; this.issues = issues;
    this.sprints = sprints; this.comments = comments; this.attachments = attachments; this.access = access;
  }

  public PageResponse<ProjectDto> list(AuthUser me, int page, int limit, boolean includeArchived) {
    List<UUID> ids = access.accessibleProjectIds(me);
    List<Project> all = projects.findAllById(ids).stream()
        .filter(p -> includeArchived || !p.isArchived())
        .sorted(Comparator.comparing(Project::getCreatedAt).reversed())
        .toList();
    int p = Math.max(page, 1), l = Math.min(Math.max(limit, 1), 100);
    int from = Math.min((p - 1) * l, all.size());
    int to = Math.min(from + l, all.size());
    List<ProjectDto> items = all.subList(from, to).stream().map(Mapper::project).toList();
    return new PageResponse<>(items, all.size(), p, l, (int) Math.ceil(all.size() / (double) l));
  }

  /** Creator becomes lead + project admin. Key must be unique, 2-10 uppercase letters. */
  @Transactional
  public ProjectDto create(ProjectCreate body, AuthUser me) {
    String key = body.key().toUpperCase();
    if (projects.existsByKeyIgnoreCase(key)) throw ApiException.badRequest("Project key already exists");

    Project p = new Project();
    p.setName(body.name());
    p.setKey(key);
    p.setDescription(body.description());
    p.setLeadUserId(body.lead_user_id() != null ? body.lead_user_id() : me.id());
    projects.save(p);

    addMemberInternal(p.getId(), me.id(), Enums.ProjectRole.admin);
    if (!p.getLeadUserId().equals(me.id())) {
      if (!users.existsById(p.getLeadUserId())) throw ApiException.badRequest("lead_user_id does not exist");
      addMemberInternal(p.getId(), p.getLeadUserId(), Enums.ProjectRole.lead);
    }
    return Mapper.project(p);
  }

  public ProjectDto get(UUID projectId, AuthUser me) {
    return Mapper.project(access.requireAccess(projectId, me));
  }

  public ProjectDto update(UUID projectId, ProjectUpdate body, AuthUser me) {
    Project p = access.requireLead(projectId, me);
    if (body.name() != null) p.setName(body.name());
    if (body.description() != null) p.setDescription(body.description());
    if (body.is_archived() != null) p.setArchived(body.is_archived());
    if (body.lead_user_id() != null) {
      if (!access.isMember(projectId, body.lead_user_id()))
        throw ApiException.badRequest("New lead must be an existing project member");
      p.setLeadUserId(body.lead_user_id());
      members.findByProjectIdAndUserId(projectId, body.lead_user_id()).ifPresent(m -> {
        if (m.getRole() == Enums.ProjectRole.member || m.getRole() == Enums.ProjectRole.viewer) {
          m.setRole(Enums.ProjectRole.lead);
          members.save(m);
        }
      });
    }
    p.setUpdatedAt(Instant.now());
    return Mapper.project(projects.save(p));
  }

  /** Hard delete cascading to sprints, issues, comments and attachments. */
  @Transactional
  public void delete(UUID projectId, AuthUser me) {
    Project p = access.projectOrThrow(projectId);
    boolean allowed = me.isGlobalAdmin() || p.getLeadUserId().equals(me.id())
        || access.isLeadOrAdmin(projectId, me);
    if (!allowed) throw ApiException.forbidden("Only the project lead or an admin can delete a project");

    for (Issue i : issues.findByProjectId(projectId)) {
      comments.deleteByIssueId(i.getId());
      attachments.deleteByIssueId(i.getId());
    }
    issues.deleteByProjectId(projectId);
    sprints.deleteByProjectId(projectId);
    members.deleteByProjectId(projectId);
    projects.delete(p);
  }

  public List<ProjectMemberDto> listMembers(UUID projectId, AuthUser me) {
    access.requireAccess(projectId, me);
    return members.findByProjectId(projectId).stream()
        .map(m -> Mapper.member(m, users.findById(m.getUserId()).orElse(null)))
        .toList();
  }

  public ProjectMemberDto addMember(UUID projectId, ProjectMemberAdd body, AuthUser me) {
    access.requireLead(projectId, me);
    UUID userId = body.resolvedUserId();
    if (userId == null) throw ApiException.badRequest("user_id is required");
    if (!users.existsById(userId)) throw ApiException.badRequest("User not found");
    if (access.isMember(projectId, userId)) throw ApiException.badRequest("User is already a member");
    ProjectMember m = addMemberInternal(projectId, userId, body.role());
    return Mapper.member(m, users.findById(userId).orElse(null));
  }

  public ProjectMemberDto updateMemberRole(UUID projectId, UUID userId, Enums.ProjectRole role, AuthUser me) {
    access.requireLead(projectId, me);
    ProjectMember m = members.findByProjectIdAndUserId(projectId, userId)
        .orElseThrow(() -> ApiException.notFound("Member not found"));
    if (m.getRole() == Enums.ProjectRole.lead && role != Enums.ProjectRole.lead && isLastLead(projectId))
      throw ApiException.badRequest("Cannot demote the only lead");
    m.setRole(role);
    members.save(m);
    return Mapper.member(m, users.findById(userId).orElse(null));
  }

  /** Cannot remove the last lead; the removed user's issues become unassigned. */
  @Transactional
  public void removeMember(UUID projectId, UUID userId, AuthUser me) {
    access.requireLead(projectId, me);
    ProjectMember m = members.findByProjectIdAndUserId(projectId, userId)
        .orElseThrow(() -> ApiException.notFound("Member not found"));
    if (m.getRole() == Enums.ProjectRole.lead && isLastLead(projectId))
      throw ApiException.badRequest("Cannot remove the only lead");

    issues.findByAssigneeIdAndProjectId(userId, projectId).forEach(i -> {
      i.setAssigneeId(null);
      i.setUpdatedAt(Instant.now());
      issues.save(i);
    });
    members.delete(m);
  }

  private boolean isLastLead(UUID projectId) {
    return members.countByProjectIdAndRole(projectId, Enums.ProjectRole.lead) <= 1;
  }

  private ProjectMember addMemberInternal(UUID projectId, UUID userId, Enums.ProjectRole role) {
    ProjectMember m = new ProjectMember();
    m.setProjectId(projectId);
    m.setUserId(userId);
    m.setRole(role);
    return members.save(m);
  }
}
