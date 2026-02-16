package edu.au.life.shortenit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {

    private String code;
    private String originalUrl;
    private Long totalClicks;
    private LocalDateTime createdAt;

    // Time-based analytics
    private Map<String, Long> clicksByDate; // date -> count
    private Map<String, Long> clicksByHour; // hour -> count

    // Geographical analytics
    private List<CountryStats> topCountries;
    private List<CityStats> topCities;

    // Device analytics
    private DeviceStats deviceStats;

    // Browser analytics
    private List<BrowserStats> topBrowsers;

    // Referrer analytics
    private List<ReferrerStats> topReferrers;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UrlResponse.UserInfo owner;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryStats {
        private String country;
        private Long clicks;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CityStats {
        private String city;
        private String country;
        private Long clicks;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceStats {
        private Long mobile;
        private Long desktop;
        private Long tablet;
        private Long unknown;
        private Double mobilePercentage;
        private Double desktopPercentage;
        private Double tabletPercentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrowserStats {
        private String browser;
        private Long clicks;
        private Double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferrerStats {
        private String referrer;
        private Long clicks;
        private Double percentage;
    }

}