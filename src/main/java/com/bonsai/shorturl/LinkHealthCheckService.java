package com.bonsai.shorturl;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

@Service
public class LinkHealthCheckService {

    private final UrlMappingRepository urlMappingRepository;

    public LinkHealthCheckService(UrlMappingRepository urlMappingRepository) {
        this.urlMappingRepository = urlMappingRepository;
    }

    // 1時間ごとの自動チェック
    @Scheduled(fixedRate = 3600000)
    public void checkAllLinks() {
        System.out.println("定期リンクチェックを開始します...");
        List<UrlMapping> allMappings = urlMappingRepository.findAll();

        for (UrlMapping mapping : allMappings) {
            checkSingleLink(mapping);
        }
        System.out.println("定期リンクチェックが完了しました。");
    }

    // 1つのリンクだけをチェックするロジック
    public void checkSingleLink(UrlMapping mapping) {
        try {
            URL url = new URL(mapping.getOriginalUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if (responseCode >= 200 && responseCode < 400) {
                mapping.setStatus(LinkStatus.OK);
            } else {
                mapping.setStatus(LinkStatus.BROKEN);
            }
        } catch (Exception e) {
            mapping.setStatus(LinkStatus.BROKEN);
        }
        urlMappingRepository.save(mapping);
    }
}