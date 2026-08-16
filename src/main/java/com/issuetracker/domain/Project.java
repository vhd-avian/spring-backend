package com.issuetracker.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "projects")
public class Project {
  @Id @GeneratedValue private UUID id;
  @Column(nullable = false) private String name;
  @Column(name = "project_key", nullable = false, unique = true) private String key;
  @Column(length = 2000) private String description;
  @Column(nullable = false) private UUID leadUserId;
  @Column(nullable = false) private boolean archived = false;
  @Column(nullable = false) private Instant createdAt = Instant.now();
  @Column(nullable = false) private Instant updatedAt = Instant.now();

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getKey() { return key; }
  public void setKey(String key) { this.key = key; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public UUID getLeadUserId() { return leadUserId; }
  public void setLeadUserId(UUID leadUserId) { this.leadUserId = leadUserId; }
  public boolean isArchived() { return archived; }
  public void setArchived(boolean archived) { this.archived = archived; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
