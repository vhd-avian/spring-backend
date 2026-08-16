package com.issuetracker.web;

import com.issuetracker.dto.Dtos.*;
import com.issuetracker.security.AuthUser;
import com.issuetracker.security.CurrentUser;
import com.issuetracker.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

  private final NotificationService notifications;

  public NotificationController(NotificationService notifications) { this.notifications = notifications; }

  @GetMapping
  public PageResponse<NotificationDto> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int limit,
                                            @RequestParam(name = "unread_only", defaultValue = "false") boolean unreadOnly,
                                            @CurrentUser AuthUser me) {
    return notifications.list(me.id(), page, limit, unreadOnly);
  }

  @PostMapping("/{notificationId}/read")
  public ResponseEntity<Void> markRead(@PathVariable UUID notificationId, @CurrentUser AuthUser me) {
    notifications.markRead(notificationId, me.id());
    return ResponseEntity.noContent().build();
  }
}
