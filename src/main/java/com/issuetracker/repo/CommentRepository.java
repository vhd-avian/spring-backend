package com.issuetracker.repo;

import com.issuetracker.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
  List<Comment> findByIssueIdOrderByCreatedAtAsc(UUID issueId);
  void deleteByIssueId(UUID issueId);
}
