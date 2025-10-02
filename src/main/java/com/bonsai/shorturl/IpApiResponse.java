package com.bonsai.shorturl;

// JSONのデータとクラスのフィールドを自動で対応させるためのアノテーション

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// @JsonIgnorePropertiesアノテーションを追加して、JSONにあってクラスにないフィールドは無視するようにする
@JsonIgnoreProperties(ignoreUnknown = true)
public class IpApiResponse {
    private String country;
    private String city;

    // Getters and Setters
    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}