package com.cookify.moderation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarningRepository extends JpaRepository<Warning, Long> {
    long countByUserId(Long userId);
    List<Warning> findByUserId(Long userId);
}
