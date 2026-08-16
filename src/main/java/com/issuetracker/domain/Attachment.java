package com.issuetracker.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "attachments")
public class Attachment {
  @Id @GeneratedValue private UUID id;
  @Column(nullable = false) private UUID issueId;
  @Column(nullable = false) private String filename;
  @Column(nullable = false) private String fileUrl;
  private String contentType;
  private long size;
  @Column(nullable = false) private UUID uploadedById;
  @Column(nullable = false) private Instant uploadedAt = Instant.now();

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getIssueId() { return issueId; }
  public void setIssueId(UUID issueId) { this.issueId = issueId; }
  public String getFilename() { return filename; }
  public void setFilename(String filename) { this.filename = filename; }
  public String getFileUrl() { return fileUrl; }
  public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }
  public long getSize() { return size; }
  public void setSize(long size) { this.size = size; }
  public UUID getUploadedById() { return uploadedById; }
  public void setUploadedById(UUID uploadedById) { this.uploadedById = uploadedById; }
  public Instant getUploadedAt() { return uploadedAt; }
  public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
}
