package com.bonsai.shorturl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    List<ClickEvent> findTop10ByUrlMappingOrderByClickTimestampDesc(UrlMapping urlMapping);

    Page<ClickEvent> findByUrlMappingOrderByClickTimestampDesc(UrlMapping urlMapping, Pageable pageable);

    List<ClickEvent> findAllByUrlMapping(UrlMapping urlMapping);
}