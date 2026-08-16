package com.issuetracker.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "sprints")
public class Sprint {
  @Id @GeneratedValue private UUID id;
  @Column(nullable = false) private UUID projectId;
  @Column(nullable = false) private String name;
  @Column(length = 1000) private String goal;
  private LocalDate startDate;
  private LocalDate endDate;
  @Column(nullable = false) private boolean active = false;
  @Column(nullable = false) private Instant createdAt = Instant.now();
  @Column(nullable = false) private Instant updatedAt = Instant.now();

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getProjectId() { return projectId; }
  public void setProjectId(UUID projectId) { this.projectId = projectId; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getGoal() { return goal; }
  public void setGoal(String goal) { this.goal = goal; }
  public LocalDate getStartDate() { return startDate; }
  public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
  public LocalDate getEndDate() { return endDate; }
  public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
