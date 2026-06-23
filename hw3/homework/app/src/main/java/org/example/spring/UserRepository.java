package org.example.spring;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    List<User> findByEmailContainingIgnoreCaseOrderByUsername(String emailPart);

    @Query("SELECT u FROM User u WHERE u.email LIKE CONCAT('%', :domain) ORDER BY u.id")
    List<User> findByEmailDomain(@Param("domain") String domain);
}
