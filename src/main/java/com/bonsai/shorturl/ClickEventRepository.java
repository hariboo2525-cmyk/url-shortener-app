package com.bonsai.shorturl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    // ★UrlMappingに関連するクリック情報を、新しい順に10件まで取得するメソッドを追加★
    List<ClickEvent> findTop10ByUrlMappingOrderByClickTimestampDesc(UrlMapping urlMapping);

    Page<ClickEvent> findByUrlMappingOrderByClickTimestampDesc(UrlMapping urlMapping, Pageable pageable);
}