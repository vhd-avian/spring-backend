package com.issuetracker.repo;

import com.issuetracker.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
  List<Attachment> findByIssueId(UUID issueId);
  void deleteByIssueId(UUID issueId);
}
