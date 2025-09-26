package com.bonsai.shorturl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;
import java.util.UUID;

@SpringBootApplication
@Controller
public class ShortUrlApplication {

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    public static void main(String[] args) {
        SpringApplication.run(ShortUrlApplication.class, args);
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/shorten")
    public String shortenUrl(@RequestParam("originalUrl") String originalUrl, Model model) {
        String shortCode;
        do {
            shortCode = UUID.randomUUID().toString().substring(0, 6);
        } while (urlMappingRepository.findByShortCode(shortCode).isPresent());

        UrlMapping urlMapping = new UrlMapping(shortCode, originalUrl);
        urlMappingRepository.save(urlMapping);

        String shortenedUrl = "http://localhost:8080/" + shortCode;
        model.addAttribute("shortenedUrl", shortenedUrl);

        return "result";
    }

    @GetMapping("/{shortCode}")
    public String redirectToOriginalUrl(@PathVariable("shortCode") String shortCode) {
        Optional<UrlMapping> urlMappingOptional = urlMappingRepository.findByShortCode(shortCode);

        if (urlMappingOptional.isPresent()) {
            UrlMapping urlMapping = urlMappingOptional.get();
            return "redirect:" + urlMapping.getOriginalUrl();
        } else {
            return "error/404";
        }
    }
}