package com.issuetracker.repo;

import com.issuetracker.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface SprintRepository extends JpaRepository<Sprint, UUID> {
  List<Sprint> findByProjectId(UUID projectId);
  Optional<Sprint> findByProjectIdAndActiveTrue(UUID projectId);
  boolean existsByProjectIdAndNameIgnoreCase(UUID projectId, String name);
  void deleteByProjectId(UUID projectId);
}
