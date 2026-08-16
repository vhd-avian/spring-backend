package com.issuetracker.web;

import com.issuetracker.dto.Dtos.*;
import com.issuetracker.security.AuthUser;
import com.issuetracker.security.CurrentUser;
import com.issuetracker.service.CommentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Comments")
@RestController
@RequestMapping("/api/v1")
public class CommentController {

  private final CommentService comments;

  public CommentController(CommentService comments) { this.comments = comments; }

  @GetMapping("/issues/{issueId}/comments")
  public List<CommentDto> list(@PathVariable UUID issueId, @CurrentUser AuthUser me) {
    return comments.list(issueId, me);
  }

  @PostMapping("/issues/{issueId}/comments")
  public ResponseEntity<CommentDto> create(@PathVariable UUID issueId,
                                           @Valid @RequestBody CommentCreate body,
                                           @CurrentUser AuthUser me) {
    return ResponseEntity.status(HttpStatus.CREATED).body(comments.create(issueId, body, me));
  }

  @PutMapping("/comments/{commentId}")
  public CommentDto update(@PathVariable UUID commentId, @Valid @RequestBody CommentUpdate body,
                           @CurrentUser AuthUser me) {
    return comments.update(commentId, body, me);
  }

  @DeleteMapping("/comments/{commentId}")
  public ResponseEntity<Void> delete(@PathVariable UUID commentId, @CurrentUser AuthUser me) {
    comments.delete(commentId, me);
    return ResponseEntity.noContent().build();
  }
}
