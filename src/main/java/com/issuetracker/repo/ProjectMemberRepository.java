package com.issuetracker.repo;

import com.issuetracker.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
  List<ProjectMember> findByProjectId(UUID projectId);
  List<ProjectMember> findByUserId(UUID userId);
  Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);
  long countByProjectIdAndRole(UUID projectId, Enums.ProjectRole role);
  void deleteByProjectId(UUID projectId);
}
