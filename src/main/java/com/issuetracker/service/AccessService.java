package com.issuetracker.service;

import com.issuetracker.domain.*;
import com.issuetracker.exception.ApiException;
import com.issuetracker.repo.*;
import com.issuetracker.security.AuthUser;
import org.springframework.stereotype.Service;

import java.util.*;

/** Central place for project-level permission rules. */
@Service
public class AccessService {

  private final ProjectRepository projects;
  private final ProjectMemberRepository members;

  public AccessService(ProjectRepository projects, ProjectMemberRepository members) {
    this.projects = projects;
    this.members = members;
  }

  public Project projectOrThrow(UUID projectId) {
    return projects.findById(projectId).orElseThrow(() -> ApiException.notFound("Project not found"));
  }

  public Optional<Enums.ProjectRole> roleOf(UUID projectId, UUID userId) {
    return members.findByProjectIdAndUserId(projectId, userId).map(ProjectMember::getRole);
  }

  /** Read access: global admin, or any project member. */
  public Project requireAccess(UUID projectId, AuthUser me) {
    Project p = projectOrThrow(projectId);
    if (me.isGlobalAdmin()) return p;
    roleOf(projectId, me.id()).orElseThrow(() -> ApiException.forbidden("No access to this project"));
    return p;
  }

  /** Write access: everything except viewer. */
  public Project requireWrite(UUID projectId, AuthUser me) {
    Project p = projectOrThrow(projectId);
    if (me.isGlobalAdmin()) return p;
    Enums.ProjectRole role = roleOf(projectId, me.id())
        .orElseThrow(() -> ApiException.forbidden("No access to this project"));
    if (role == Enums.ProjectRole.viewer) throw ApiException.forbidden("Viewers cannot modify project data");
    return p;
  }

  /** Admin/lead access. */
  public Project requireLead(UUID projectId, AuthUser me) {
    Project p = projectOrThrow(projectId);
    if (me.isGlobalAdmin()) return p;
    Enums.ProjectRole role = roleOf(projectId, me.id())
        .orElseThrow(() -> ApiException.forbidden("No access to this project"));
    if (role != Enums.ProjectRole.lead && role != Enums.ProjectRole.admin)
      throw ApiException.forbidden("Requires project lead or admin");
    return p;
  }

  public boolean isLeadOrAdmin(UUID projectId, AuthUser me) {
    if (me.isGlobalAdmin()) return true;
    return roleOf(projectId, me.id())
        .map(r -> r == Enums.ProjectRole.lead || r == Enums.ProjectRole.admin)
        .orElse(false);
  }

  public boolean isMember(UUID projectId, UUID userId) {
    return members.findByProjectIdAndUserId(projectId, userId).isPresent();
  }

  public List<UUID> accessibleProjectIds(AuthUser me) {
    if (me.isGlobalAdmin()) return projects.findAll().stream().map(Project::getId).toList();
    return members.findByUserId(me.id()).stream().map(ProjectMember::getProjectId).toList();
  }
}
