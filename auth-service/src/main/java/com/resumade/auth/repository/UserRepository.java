package com.resumade.auth.repository;

import com.resumade.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByRole(User.Role role);
    List<User> findBySubscriptionPlan(User.SubscriptionPlan plan);
    long countBySubscriptionPlan(User.SubscriptionPlan plan);
    long countByIsActiveTrue();
    Optional<User> findByResetToken(String resetToken);
}
