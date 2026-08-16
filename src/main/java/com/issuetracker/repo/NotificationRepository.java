package com.issuetracker.repo;

import com.issuetracker.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
  Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
  Page<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
