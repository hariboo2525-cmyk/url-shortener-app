package com.bonsai.shorturl;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableScheduling
@Controller
public class ShortUrlApplication {

    private final UrlMappingRepository urlMappingRepository;
    private final ClickEventRepository clickEventRepository;
    private final UserRepository userRepository;
    private final LinkHealthCheckService linkHealthCheckService;

    public ShortUrlApplication(UrlMappingRepository urlMappingRepository, ClickEventRepository clickEventRepository, UserRepository userRepository, LinkHealthCheckService linkHealthCheckService) {
        this.urlMappingRepository = urlMappingRepository;
        this.clickEventRepository = clickEventRepository;
        this.userRepository = userRepository;
        this.linkHealthCheckService = linkHealthCheckService;
    }

    public static void main(String[] args) {
        SpringApplication.run(ShortUrlApplication.class, args);
    }

    // (他のメソッドは変更なし)
    // ...

    @GetMapping("/")
    public String handleRootAccess(@AuthenticationPrincipal User user) {
        if (user != null) {
            return "redirect:/dashboard";
        }
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal User user) {
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("username", user.getUsername());
        List<UrlMapping> userUrls = urlMappingRepository.findAllByUser(user);
        model.addAttribute("urls", userUrls);
        return "dashboard";
    }


    @PostMapping("/shorten")
    public String shortenUrl(@RequestParam("originalUrl") String originalUrl,
                             @RequestParam(value = "customCode", required = false) String customCode,
                             @RequestParam(value = "expirationTimestamp", required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expirationTimestamp,
                             @AuthenticationPrincipal User user,
                             RedirectAttributes redirectAttributes) {

        if (user == null) {
            return "redirect:/login";
        }

        String shortCode;
        if (customCode != null && !customCode.trim().isEmpty()) {
            if (urlMappingRepository.findByShortCode(customCode).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "このカスタムURLはすでに使用されています: " + customCode);
                return "redirect:/dashboard";
            }
            shortCode = customCode.trim();
        } else {
            do {
                shortCode = UUID.randomUUID().toString().substring(0, 6);
            } while (urlMappingRepository.findByShortCode(shortCode).isPresent());
        }

        UrlMapping urlMapping = new UrlMapping(shortCode, originalUrl);
        if (expirationTimestamp != null) {
            urlMapping.setExpirationTimestamp(expirationTimestamp);
        }
        urlMapping.setUser(user);
        urlMappingRepository.save(urlMapping);

        return "redirect:/dashboard";
    }

    @GetMapping("/{shortCode}")
    public String redirectToOriginalUrl(@PathVariable("shortCode") String shortCode, HttpServletRequest request) {
        Optional<UrlMapping> urlMappingOptional = urlMappingRepository.findByShortCode(shortCode);

        if (urlMappingOptional.isPresent()) {
            UrlMapping urlMapping = urlMappingOptional.get();

            if (urlMapping.getExpirationTimestamp() != null && LocalDateTime.now().isAfter(urlMapping.getExpirationTimestamp())) {
                return "expired";
            }

            String ipAddress = request.getRemoteAddr();
            if ("0:0:0:0:0:0:0:1".equals(ipAddress) || "127.0.0.1".equals(ipAddress)) {
                ipAddress = "8.8.8.8";
            }

            String apiUrl = "http://ip-api.com/json/" + ipAddress;
            RestTemplate restTemplate = new RestTemplate();
            IpApiResponse response = restTemplate.getForObject(apiUrl, IpApiResponse.class);

            String country = (response != null && response.getCountry() != null) ? response.getCountry() : "Unknown";
            String city = (response != null && response.getCity() != null) ? response.getCity() : "Unknown";

            String referrer = request.getHeader("Referer");
            if (referrer == null || referrer.isEmpty()) {
                referrer = "Direct";
            }

            String userAgent = request.getHeader("User-Agent");
            String deviceType = "Desktop";
            if (userAgent != null && (userAgent.toLowerCase().contains("mobile") || userAgent.toLowerCase().contains("android") || userAgent.toLowerCase().contains("iphone"))) {
                deviceType = "Mobile";
            }

            ClickEvent clickEvent = new ClickEvent(urlMapping, LocalDateTime.now(), country, city, referrer, deviceType, ipAddress);
            clickEventRepository.save(clickEvent);

            urlMapping.incrementClickCount();
            urlMappingRepository.save(urlMapping);

            return "redirect:" + urlMapping.getOriginalUrl();
        } else {
            return "error/404";
        }
    }


    @GetMapping("/analytics/{shortCode}")
    public String showAnalytics(@PathVariable("shortCode") String shortCode,
                                @RequestParam(defaultValue = "0") int page,
                                @AuthenticationPrincipal User currentUser,
                                Model model) {
        Optional<UrlMapping> urlMappingOptional = urlMappingRepository.findByShortCode(shortCode);

        if (urlMappingOptional.isPresent()) {
            UrlMapping urlMapping = urlMappingOptional.get();
            if (currentUser == null || urlMapping.getUser() == null || !urlMapping.getUser().getId().equals(currentUser.getId())) {
                return "error/404";
            }

            model.addAttribute("urlMapping", urlMapping);
            Pageable pageable = PageRequest.of(page, 10);
            Page<ClickEvent> clickEventsPage = clickEventRepository.findByUrlMappingOrderByClickTimestampDesc(urlMapping, pageable);
            model.addAttribute("clickEventsPage", clickEventsPage);

            List<ClickEvent> allClickEvents = clickEventRepository.findAllByUrlMapping(urlMapping);

            long uniqueUserCount = allClickEvents.stream()
                    .map(ClickEvent::getIpAddress)
                    .distinct()
                    .count();
            model.addAttribute("uniqueUserCount", uniqueUserCount);

            long desktopCount = allClickEvents.stream().filter(e -> "Desktop".equals(e.getDeviceType())).count();
            long mobileCount = allClickEvents.stream().filter(e -> "Mobile".equals(e.getDeviceType())).count();
            model.addAttribute("desktopCount", desktopCount);
            model.addAttribute("mobileCount", mobileCount);

            // ▼▼▼ 日別クリック数の計算ロジックをここから変更 ▼▼▼

            // 1. まずは今まで通り、日付ごとのクリック数を集計しておく
            Map<String, Long> clicksByDate = allClickEvents.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getClickTimestamp().toLocalDate().format(DateTimeFormatter.ofPattern("MM/dd")),
                            Collectors.counting()
                    ));

            // 2. グラフに表示するラベルとデータを入れるための空のリストを用意
            List<String> dateLabels = new ArrayList<>();
            List<Long> clickCounts = new ArrayList<>();

            // 3. 今日から3日後までの4日間をループ処理
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
            for (int i = 0; i < 4; i++) {
                LocalDate currentDate = today.plusDays(i);
                String formattedDate = currentDate.format(formatter);

                // ラベルリストに日付を追加
                dateLabels.add(formattedDate);

                // 集計したマップから該当日付のクリック数を取得（なければ0）して、データリストに追加
                clickCounts.add(clicksByDate.getOrDefault(formattedDate, 0L));
            }

            model.addAttribute("dateLabels", dateLabels);
            model.addAttribute("clickCounts", clickCounts);

            // ▲▲▲ 日別クリック数の計算ロジックの変更はここまで ▲▲▲

            return "analytics";
        } else {
            return "error/404";
        }
    }

    @GetMapping("/edit/{shortCode}")
    public String showEditPage(@PathVariable("shortCode") String shortCode,
                               @AuthenticationPrincipal User currentUser,
                               Model model) {
        Optional<UrlMapping> urlMappingOptional = urlMappingRepository.findByShortCode(shortCode);
        if (urlMappingOptional.isPresent()) {
            UrlMapping urlMapping = urlMappingOptional.get();
            if (currentUser == null || urlMapping.getUser() == null || !urlMapping.getUser().getId().equals(currentUser.getId())) {
                return "error/404";
            }
            model.addAttribute("urlMapping", urlMapping);
            return "edit";
        } else {
            return "error/404";
        }
    }

    @PostMapping("/update")
    public String updateUrl(@RequestParam("shortCode") String shortCode,
                            @RequestParam("originalUrl") String newOriginalUrl,
                            @AuthenticationPrincipal User currentUser,
                            RedirectAttributes redirectAttributes) {
        Optional<UrlMapping> urlMappingOptional = urlMappingRepository.findByShortCode(shortCode);
        if (urlMappingOptional.isPresent()) {
            UrlMapping urlMapping = urlMappingOptional.get();
            if (currentUser == null || urlMapping.getUser() == null || !urlMapping.getUser().getId().equals(currentUser.getId())) {
                return "error/404";
            }
            urlMapping.setOriginalUrl(newOriginalUrl);
            urlMappingRepository.save(urlMapping);
            return "redirect:/analytics/" + shortCode;
        } else {
            return "error/404";
        }
    }

    @GetMapping("/check/{shortCode}")
    public RedirectView checkLinkNow(@PathVariable("shortCode") String shortCode, @AuthenticationPrincipal User currentUser) {
        Optional<UrlMapping> urlMappingOptional = urlMappingRepository.findByShortCode(shortCode);
        if (urlMappingOptional.isPresent()) {
            UrlMapping urlMapping = urlMappingOptional.get();
            if (currentUser != null && urlMapping.getUser() != null && urlMapping.getUser().getId().equals(currentUser.getId())) {
                linkHealthCheckService.checkSingleLink(urlMapping);
            }
        }
        return new RedirectView("/dashboard");
    }
}