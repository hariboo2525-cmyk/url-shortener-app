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

    public ClickEvent() {
    }

    public ClickEvent(UrlMapping urlMapping, LocalDateTime clickTimestamp, String country, String city, String referrer, String deviceType) {
        this.urlMapping = urlMapping;
        this.clickTimestamp = clickTimestamp;
        this.country = country;
        this.city = city;
        this.referrer = referrer;
        this.deviceType = deviceType;
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
}