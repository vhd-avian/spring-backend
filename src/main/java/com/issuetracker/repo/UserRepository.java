package com.issuetracker.repo;

import com.issuetracker.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmailIgnoreCase(String email);
  boolean existsByEmailIgnoreCase(String email);
  List<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String a, String b, Pageable p);
}
