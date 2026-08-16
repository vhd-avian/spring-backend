package com.issuetracker.service;

import com.issuetracker.domain.*;
import com.issuetracker.dto.Dtos.*;

import java.util.List;

public class Mapper {

  public static UserDto user(User u) {
    if (u == null) return null;
    return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getAvatarUrl(),
        u.getBio(), u.getPhoneNumber(), u.getGlobalRole().name(), u.getCreatedAt(), u.getUpdatedAt());
  }

  public static ProjectDto project(Project p) {
    return new ProjectDto(p.getId(), p.getName(), p.getKey(), p.getDescription(),
        p.getLeadUserId(), p.isArchived(), p.getCreatedAt(), p.getUpdatedAt());
  }

  public static ProjectMemberDto member(ProjectMember m, User u) {
    return new ProjectMemberDto(m.getProjectId(), m.getUserId(), m.getRole().name(), user(u));
  }

  public static SprintDto sprint(Sprint s) {
    return new SprintDto(s.getId(), s.getProjectId(), s.getName(), s.getGoal(),
        s.getStartDate(), s.getEndDate(), s.isActive(), s.getCreatedAt(), s.getUpdatedAt());
  }

  public static IssueDto issue(Issue i) { return issue(i, null, null); }

  public static IssueDto issue(Issue i, User assignee, User reporter) {
    return new IssueDto(i.getId(), i.getTitle(), i.getDescription(), i.getType().name(),
        i.getStatus().name(), i.getPriority().name(), i.getStoryPoints(), i.getAssigneeId(),
        i.getReporterId(), i.getProjectId(), i.getSprintId(), i.getParentIssueId(),
        i.getDueDate(), i.getCreatedAt(), i.getUpdatedAt(), user(assignee), user(reporter));
  }

  public static IssueDetailDto issueDetail(Issue i, User assignee, User reporter,
                                           List<CommentDto> comments, List<AttachmentDto> attachments) {
    return new IssueDetailDto(i.getId(), i.getTitle(), i.getDescription(), i.getType().name(),
        i.getStatus().name(), i.getPriority().name(), i.getStoryPoints(), i.getAssigneeId(),
        i.getReporterId(), i.getProjectId(), i.getSprintId(), i.getParentIssueId(),
        i.getDueDate(), i.getCreatedAt(), i.getUpdatedAt(), user(assignee), user(reporter),
        comments, attachments);
  }

  public static CommentDto comment(Comment c, User author) {
    return new CommentDto(c.getId(), c.getIssueId(), c.getAuthorId(), c.getContent(),
        c.getCreatedAt(), c.getUpdatedAt(), user(author));
  }

  public static AttachmentDto attachment(Attachment a) { return attachment(a, null); }

  public static AttachmentDto attachment(Attachment a, User uploadedBy) {
    return new AttachmentDto(a.getId(), a.getIssueId(), a.getFilename(), a.getFileUrl(),
        a.getUploadedById(), a.getUploadedAt(), user(uploadedBy));
  }

  public static NotificationDto notification(Notification n) {
    return new NotificationDto(n.getId(), n.getUserId(), n.getType(), n.getMessage(),
        n.getIssueId(), n.isRead(), n.getCreatedAt());
  }
}
