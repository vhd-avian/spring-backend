package com.issuetracker.web;

import com.issuetracker.dto.Dtos.*;
import com.issuetracker.security.AuthUser;
import com.issuetracker.security.CurrentUser;
import com.issuetracker.service.AttachmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Attachments")
@RestController
@RequestMapping("/api/v1")
public class AttachmentController {

  private final AttachmentService attachments;

  public AttachmentController(AttachmentService attachments) { this.attachments = attachments; }

  @GetMapping("/issues/{issueId}/attachments")
  public List<AttachmentDto> list(@PathVariable UUID issueId, @CurrentUser AuthUser me) {
    return attachments.list(issueId, me);
  }

  @PostMapping(value = "/issues/{issueId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<AttachmentDto> upload(@PathVariable UUID issueId,
                                              @RequestPart("file") MultipartFile file,
                                              @CurrentUser AuthUser me) {
    return ResponseEntity.status(HttpStatus.CREATED).body(attachments.upload(issueId, file, me));
  }

  @GetMapping("/attachments/{attachmentId}")
  public ResponseEntity<Void> download(@PathVariable UUID attachmentId, @CurrentUser AuthUser me) {
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(attachments.downloadUrl(attachmentId, me)))
        .build();
  }

  @DeleteMapping("/attachments/{attachmentId}")
  public ResponseEntity<Void> delete(@PathVariable UUID attachmentId, @CurrentUser AuthUser me) {
    attachments.delete(attachmentId, me);
    return ResponseEntity.noContent().build();
  }
}
