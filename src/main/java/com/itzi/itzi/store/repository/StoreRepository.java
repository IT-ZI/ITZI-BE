package com.itzi.itzi.store.repository;

import com.itzi.itzi.store.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findByUser_UserId(Long userId);
}
