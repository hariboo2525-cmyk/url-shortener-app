package com.bonsai.shorturl;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {
    Optional<UrlMapping> findByShortCode(String shortCode);

    List<UrlMapping> findAllByUser(User user);
}