package com.issuetracker.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "project_members", uniqueConstraints = @UniqueConstraint(columnNames = {"projectId", "userId"}))
public class ProjectMember {
  @Id @GeneratedValue private UUID id;
  @Column(nullable = false) private UUID projectId;
  @Column(nullable = false) private UUID userId;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private Enums.ProjectRole role;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getProjectId() { return projectId; }
  public void setProjectId(UUID projectId) { this.projectId = projectId; }
  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }
  public Enums.ProjectRole getRole() { return role; }
  public void setRole(Enums.ProjectRole role) { this.role = role; }
}
