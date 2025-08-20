package com.itzi.itzi.auth.repository;

import com.itzi.itzi.auth.domain.OrgProfile;
import com.itzi.itzi.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrgProfileRepository extends JpaRepository<OrgProfile, Long> {

    // User pk로 OrgProfile 조회
    Optional<OrgProfile> findByUser_UserId(Long userId);
    Optional<OrgProfile> findByUser(User user);
}