package com.issuetracker.service;

import com.issuetracker.domain.*;
import com.issuetracker.dto.Dtos.*;
import com.issuetracker.exception.ApiException;
import com.issuetracker.repo.*;
import com.issuetracker.security.AuthUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class AttachmentService {

  private static final long MAX_SIZE = 10L * 1024 * 1024;

  private final AttachmentRepository attachments;
  private final IssueRepository issues;
  private final AccessService access;
  private final UserRepository users;
  private final Path uploadDir;

  public AttachmentService(AttachmentRepository attachments, IssueRepository issues, AccessService access,
                           UserRepository users,
                           @Value("${app.uploads.dir:./uploads}") String dir) {
    this.attachments = attachments; this.issues = issues; this.access = access; this.users = users;
    this.uploadDir = Paths.get(dir).toAbsolutePath();
  }

  public List<AttachmentDto> list(UUID issueId, AuthUser me) {
    Issue i = issueOrThrow(issueId);
    access.requireAccess(i.getProjectId(), me);
    return attachments.findByIssueId(issueId).stream()
        .map(x -> Mapper.attachment(x, users.findById(x.getUploadedById()).orElse(null))).toList();
  }

  /** Max 10 MB, only image/*, application/pdf and text/plain. */
  public AttachmentDto upload(UUID issueId, MultipartFile file, AuthUser me) {
    Issue i = issueOrThrow(issueId);
    access.requireWrite(i.getProjectId(), me);

    if (file == null || file.isEmpty()) throw ApiException.badRequest("File is required");
    if (file.getSize() > MAX_SIZE) throw ApiException.badRequest("File exceeds the 10 MB limit");
    String contentType = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");
    boolean allowed = contentType.startsWith("image/")
        || contentType.equals("application/pdf")
        || contentType.equals("text/plain");
    if (!allowed) throw ApiException.badRequest("Unsupported file type: " + contentType);

    String original = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
    String safeName = UUID.randomUUID() + "-" + Paths.get(original).getFileName().toString();
    try {
      Files.createDirectories(uploadDir);
      Files.copy(file.getInputStream(), uploadDir.resolve(safeName), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file");
    }

    Attachment a = new Attachment();
    a.setIssueId(issueId);
    a.setFilename(original);
    a.setFileUrl("/api/v1/files/" + safeName);
    a.setContentType(contentType);
    a.setSize(file.getSize());
    a.setUploadedById(me.id());
    return Mapper.attachment(attachments.save(a), users.findById(me.id()).orElse(null));
  }

  public String downloadUrl(UUID attachmentId, AuthUser me) {
    Attachment a = attachmentOrThrow(attachmentId);
    Issue i = issueOrThrow(a.getIssueId());
    access.requireAccess(i.getProjectId(), me);
    return a.getFileUrl();
  }

  /** Uploader, project lead or admin. */
  public void delete(UUID attachmentId, AuthUser me) {
    Attachment a = attachmentOrThrow(attachmentId);
    Issue i = issueOrThrow(a.getIssueId());
    boolean allowed = a.getUploadedById().equals(me.id()) || access.isLeadOrAdmin(i.getProjectId(), me);
    if (!allowed) throw ApiException.forbidden("Not allowed to delete this attachment");
    attachments.delete(a);
  }

  private Issue issueOrThrow(UUID id) {
    return issues.findById(id).orElseThrow(() -> ApiException.notFound("Issue not found"));
  }

  private Attachment attachmentOrThrow(UUID id) {
    return attachments.findById(id).orElseThrow(() -> ApiException.notFound("Attachment not found"));
  }
}
