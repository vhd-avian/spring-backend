package com.issuetracker.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "comments")
public class Comment {
  @Id @GeneratedValue private UUID id;
  @Column(nullable = false) private UUID issueId;
  @Column(nullable = false) private UUID authorId;
  @Column(nullable = false, length = 5000) private String content;
  @Column(nullable = false) private Instant createdAt = Instant.now();
  @Column(nullable = false) private Instant updatedAt = Instant.now();

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getIssueId() { return issueId; }
  public void setIssueId(UUID issueId) { this.issueId = issueId; }
  public UUID getAuthorId() { return authorId; }
  public void setAuthorId(UUID authorId) { this.authorId = authorId; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
