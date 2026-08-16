package com.issuetracker.repo;

import com.issuetracker.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface IssueRepository extends JpaRepository<Issue, UUID>, JpaSpecificationExecutor<Issue> {
  List<Issue> findByProjectId(UUID projectId);
  List<Issue> findBySprintId(UUID sprintId);
  List<Issue> findByParentIssueId(UUID parentIssueId);
  List<Issue> findByAssigneeIdAndProjectId(UUID assigneeId, UUID projectId);
  void deleteByProjectId(UUID projectId);
}
