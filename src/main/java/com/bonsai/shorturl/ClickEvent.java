package com.bonsai.shorturl;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private UrlMapping urlMapping;

    private LocalDateTime clickTimestamp;

    private String country;
    private String city;
    private String referrer;
    private String deviceType;
    private String ipAddress; // ★ IPアドレスを保存する変数を追加！

    public ClickEvent() {
    }

    // ★ コンストラクタに ipAddress を追加！
    public ClickEvent(UrlMapping urlMapping, LocalDateTime clickTimestamp, String country, String city, String referrer, String deviceType, String ipAddress) {
        this.urlMapping = urlMapping;
        this.clickTimestamp = clickTimestamp;
        this.country = country;
        this.city = city;
        this.referrer = referrer;
        this.deviceType = deviceType;
        this.ipAddress = ipAddress; // ★ 追加
    }

    public Long getId() {
        return id;
    }

    public UrlMapping getUrlMapping() {
        return urlMapping;
    }

    public LocalDateTime getClickTimestamp() {
        return clickTimestamp;
    }

    public String getCountry() {
        return country;
    }

    public String getCity() {
        return city;
    }

    public String getReferrer() {
        return referrer;
    }

    public String getDeviceType() {
        return deviceType;
    }

    // ★ ipAddress のための Getter を追加！
    public String getIpAddress() {
        return ipAddress;
    }
}