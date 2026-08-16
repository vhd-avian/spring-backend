package com.issuetracker.service;

import com.issuetracker.domain.Notification;
import com.issuetracker.dto.Dtos.*;
import com.issuetracker.exception.ApiException;
import com.issuetracker.repo.NotificationRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationService {

  private final NotificationRepository notifications;

  public NotificationService(NotificationRepository notifications) { this.notifications = notifications; }

  public void notify(UUID userId, String type, String message, UUID issueId) {
    if (userId == null) return;
    Notification n = new Notification();
    n.setUserId(userId);
    n.setType(type);
    n.setMessage(message);
    n.setIssueId(issueId);
    notifications.save(n);
  }

  public PageResponse<NotificationDto> list(UUID userId, int page, int limit, boolean unreadOnly) {
    Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(limit, 1), 100));
    Page<Notification> result = unreadOnly
        ? notifications.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable)
        : notifications.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    return new PageResponse<>(result.getContent().stream().map(Mapper::notification).toList(),
        result.getTotalElements(), page, limit, result.getTotalPages());
  }

  public void markRead(UUID notificationId, UUID userId) {
    Notification n = notifications.findById(notificationId)
        .orElseThrow(() -> ApiException.notFound("Notification not found"));
    if (!n.getUserId().equals(userId)) throw ApiException.forbidden("Not your notification");
    n.setRead(true);
    notifications.save(n);
  }
}
