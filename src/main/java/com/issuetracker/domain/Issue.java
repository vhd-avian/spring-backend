package com.issuetracker.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "issues")
public class Issue {
  @Id @GeneratedValue private UUID id;
  @Column(nullable = false) private String title;
  @Column(length = 5000) private String description;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private Enums.IssueType type;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private Enums.IssueStatus status = Enums.IssueStatus.backlog;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private Enums.Priority priority = Enums.Priority.medium;
  private Integer storyPoints;
  private UUID assigneeId;
  @Column(nullable = false) private UUID reporterId;
  @Column(nullable = false) private UUID projectId;
  private UUID sprintId;
  private UUID parentIssueId;
  private LocalDate dueDate;
  @Column(nullable = false) private Instant createdAt = Instant.now();
  @Column(nullable = false) private Instant updatedAt = Instant.now();

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public Enums.IssueType getType() { return type; }
  public void setType(Enums.IssueType type) { this.type = type; }
  public Enums.IssueStatus getStatus() { return status; }
  public void setStatus(Enums.IssueStatus status) { this.status = status; }
  public Enums.Priority getPriority() { return priority; }
  public void setPriority(Enums.Priority priority) { this.priority = priority; }
  public Integer getStoryPoints() { return storyPoints; }
  public void setStoryPoints(Integer storyPoints) { this.storyPoints = storyPoints; }
  public UUID getAssigneeId() { return assigneeId; }
  public void setAssigneeId(UUID assigneeId) { this.assigneeId = assigneeId; }
  public UUID getReporterId() { return reporterId; }
  public void setReporterId(UUID reporterId) { this.reporterId = reporterId; }
  public UUID getProjectId() { return projectId; }
  public void setProjectId(UUID projectId) { this.projectId = projectId; }
  public UUID getSprintId() { return sprintId; }
  public void setSprintId(UUID sprintId) { this.sprintId = sprintId; }
  public UUID getParentIssueId() { return parentIssueId; }
  public void setParentIssueId(UUID parentIssueId) { this.parentIssueId = parentIssueId; }
  public LocalDate getDueDate() { return dueDate; }
  public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
