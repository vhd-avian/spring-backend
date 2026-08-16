package com.issuetracker.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "notifications")
public class Notification {
  @Id @GeneratedValue private UUID id;
  @Column(nullable = false) private UUID userId;
  @Column(nullable = false) private String type;      // issue_assigned | comment_mention | status_changed
  @Column(nullable = false) private String message;
  private UUID issueId;
  @Column(name = "is_read", nullable = false) private boolean read = false;
  @Column(nullable = false) private Instant createdAt = Instant.now();

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getUserId() { return userId; }
  public void setUserId(UUID userId) { this.userId = userId; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }
  public UUID getIssueId() { return issueId; }
  public void setIssueId(UUID issueId) { this.issueId = issueId; }
  public boolean isRead() { return read; }
  public void setRead(boolean read) { this.read = read; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
