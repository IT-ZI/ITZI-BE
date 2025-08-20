package com.itzi.itzi.auth.repository;

import com.itzi.itzi.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
