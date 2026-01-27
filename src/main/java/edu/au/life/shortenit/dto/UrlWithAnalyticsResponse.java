package edu.au.life.shortenit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlWithAnalyticsResponse {
    private String code;
    private String originalUrl;
    private String customAlias;
    private Long clickCount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Boolean isExpired;
    private Boolean isActive;
    private AnalyticsSummary analyticsSummary;
    private String title;
    private String ownerName;
    private String ownerEmail;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyticsSummary {
        private Long totalClicks;
        private LocalDateTime lastClickedAt;
        private String topCountry;
        private Long topCountryClicks;
        private String topDeviceType;
        private Long topDeviceClicks;
        private Long clicksToday;
        private Long clicksThisWeek;
    }
}