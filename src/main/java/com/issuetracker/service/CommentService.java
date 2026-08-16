package com.issuetracker.service;

import com.issuetracker.domain.*;
import com.issuetracker.dto.Dtos.*;
import com.issuetracker.exception.ApiException;
import com.issuetracker.repo.*;
import com.issuetracker.security.AuthUser;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.regex.*;

@Service
public class CommentService {

  private static final Pattern MENTION = Pattern.compile("@([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");

  private final CommentRepository comments;
  private final IssueRepository issues;
  private final UserRepository users;
  private final AccessService access;
  private final NotificationService notifications;

  public CommentService(CommentRepository comments, IssueRepository issues, UserRepository users,
                        AccessService access, NotificationService notifications) {
    this.comments = comments; this.issues = issues; this.users = users;
    this.access = access; this.notifications = notifications;
  }

  public List<CommentDto> list(UUID issueId, AuthUser me) {
    Issue i = issueOrThrow(issueId);
    access.requireAccess(i.getProjectId(), me);
    return comments.findByIssueIdOrderByCreatedAtAsc(issueId).stream()
        .map(c -> Mapper.comment(c, users.findById(c.getAuthorId()).orElse(null)))
        .toList();
  }

  public CommentDto create(UUID issueId, CommentCreate body, AuthUser me) {
    Issue i = issueOrThrow(issueId);
    access.requireWrite(i.getProjectId(), me);

    Comment c = new Comment();
    c.setIssueId(issueId);
    c.setAuthorId(me.id());
    c.setContent(body.content());
    comments.save(c);

    notifyMentions(body.content(), i, me);
    return Mapper.comment(c, users.findById(me.id()).orElse(null));
  }

  /** Only the author may edit. */
  public CommentDto update(UUID commentId, CommentUpdate body, AuthUser me) {
    Comment c = commentOrThrow(commentId);
    if (!c.getAuthorId().equals(me.id())) throw ApiException.forbidden("Only the author can edit this comment");
    c.setContent(body.content());
    c.setUpdatedAt(Instant.now());
    comments.save(c);
    return Mapper.comment(c, users.findById(c.getAuthorId()).orElse(null));
  }

  /** Author, project lead or admin may delete. */
  public void delete(UUID commentId, AuthUser me) {
    Comment c = commentOrThrow(commentId);
    Issue i = issueOrThrow(c.getIssueId());
    boolean allowed = c.getAuthorId().equals(me.id()) || access.isLeadOrAdmin(i.getProjectId(), me);
    if (!allowed) throw ApiException.forbidden("Not allowed to delete this comment");
    comments.delete(c);
  }

  private void notifyMentions(String content, Issue issue, AuthUser me) {
    Matcher m = MENTION.matcher(content);
    Set<UUID> notified = new HashSet<>();
    while (m.find()) {
      users.findByEmailIgnoreCase(m.group(1)).ifPresent(u -> {
        if (!u.getId().equals(me.id()) && notified.add(u.getId()))
          notifications.notify(u.getId(), "comment_mention",
              "You were mentioned on issue: " + issue.getTitle(), issue.getId());
      });
    }
  }

  private Issue issueOrThrow(UUID id) {
    return issues.findById(id).orElseThrow(() -> ApiException.notFound("Issue not found"));
  }

  private Comment commentOrThrow(UUID id) {
    return comments.findById(id).orElseThrow(() -> ApiException.notFound("Comment not found"));
  }
}
