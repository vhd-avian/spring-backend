package com.issuetracker.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "users")
public class User {
  @Id @GeneratedValue private UUID id;
  @Column(nullable = false, unique = true) private String email;
  @Column(nullable = false) private String passwordHash;
  @Column(nullable = false) private String fullName;
  private String avatarUrl;
  @Column(length = 2000) private String bio;
  private String phoneNumber;
  @Enumerated(EnumType.STRING) @Column(nullable = false)
  private Enums.GlobalRole globalRole = Enums.GlobalRole.user;
  @Column(nullable = false) private Instant createdAt = Instant.now();
  private Instant updatedAt;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getPasswordHash() { return passwordHash; }
  public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
  public String getFullName() { return fullName; }
  public void setFullName(String fullName) { this.fullName = fullName; }
  public String getAvatarUrl() { return avatarUrl; }
  public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
  public Enums.GlobalRole getGlobalRole() { return globalRole; }
  public void setGlobalRole(Enums.GlobalRole globalRole) { this.globalRole = globalRole; }
  public String getBio() { return bio; }
  public void setBio(String bio) { this.bio = bio; }
  public String getPhoneNumber() { return phoneNumber; }
  public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
