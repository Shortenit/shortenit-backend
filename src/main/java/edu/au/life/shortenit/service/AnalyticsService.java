package edu.au.life.shortenit.service;

import edu.au.life.shortenit.dto.AnalyticsResponse;
import edu.au.life.shortenit.entity.Url;
import edu.au.life.shortenit.entity.UrlClick;
import edu.au.life.shortenit.exception.UrlNotFoundException;
import edu.au.life.shortenit.repository.UrlClickRepository;
import edu.au.life.shortenit.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final UrlRepository urlRepository;
    private final UrlClickRepository urlClickRepository;

    public AnalyticsResponse getAnalytics(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short Url not found " + shortCode));

        List<UrlClick> clicks = urlClickRepository.findByUrlOrderByClickedAtDesc(url);

        return AnalyticsResponse.builder()
                .shortCode(url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .totalClicks(url.getClickCount())
                .createdAt(url.getCreatedAt())
                .clicksByDate(getClicksByDate(clicks))
                .clicksByHour(getClicksByHour(clicks))
                .topCountries(getTopCountries(clicks))
                .topCities(getTopCities(clicks))
                .deviceStats(getDeviceStats(clicks))
                .topBrowsers(getTopBrowsers(clicks))
                .topReferrers(getTopReferrers(clicks))
                .recentClicks(getRecentClicks(clicks, 10))
                .build();
    }

    private Map<String, Long> getClicksByDate(List<UrlClick> clicks) {
        return clicks.stream()
                .collect(Collectors.groupingBy(
                        click -> click.getClickedAt().toLocalDate().toString(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private Map<String, Long> getClicksByHour(List<UrlClick> clicks) {
        return clicks.stream()
                .collect(Collectors.groupingBy(
                        click -> String.valueOf(click.getClickedAt().getHour()),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private List<AnalyticsResponse.CountryStats> getTopCountries(List<UrlClick> clicks) {
        long totalClicks = clicks.size();

        Map<String, Long> countryMap = clicks.stream()
                .filter(click -> click.getCountry() != null)
                .collect(Collectors.groupingBy(
                        UrlClick::getCountry,
                        Collectors.counting()
                ));

        return countryMap.entrySet().stream()
                .map(entry -> AnalyticsResponse.CountryStats.builder()
                        .country(entry.getKey())
                        .clicks(entry.getValue())
                        .percentage(totalClicks > 0 ? (entry.getValue() * 100.0 / totalClicks) : 0.0)
                        .build())
                .sorted((a, b) -> Long.compare(b.getClicks(), a.getClicks()))
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<AnalyticsResponse.CityStats> getTopCities(List<UrlClick> clicks) {
        long totalClicks = clicks.size();

        return clicks.stream()
                .filter(click -> click.getCity() != null)
                .collect(Collectors.groupingBy(
                        click -> click.getCity() + "|" + click.getCountry(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|");
                    return AnalyticsResponse.CityStats.builder()
                            .city(parts[0])
                            .country(parts.length > 1 ? parts[1] : "Unknown")
                            .clicks(entry.getValue())
                            .percentage(totalClicks > 0 ? (entry.getValue() * 100.0 / totalClicks) : 0.0)
                            .build();
                })
                .sorted((a, b) -> Long.compare(b.getClicks(), a.getClicks()))
                .limit(10)
                .collect(Collectors.toList());
    }

    private AnalyticsResponse.DeviceStats getDeviceStats(List<UrlClick> clicks) {
        Map<String, Long> deviceMap = clicks.stream()
                .collect(Collectors.groupingBy(
                        click -> click.getDeviceType() != null ? click.getDeviceType() : "unknown",
                        Collectors.counting()
                ));

        long mobile = deviceMap.getOrDefault("mobile", 0L);
        long desktop = deviceMap.getOrDefault("desktop", 0L);
        long tablet = deviceMap.getOrDefault("tablet", 0L);
        long unknown = deviceMap.getOrDefault("unknown", 0L);
        long total = clicks.size();

        return AnalyticsResponse.DeviceStats.builder()
                .mobile(mobile)
                .desktop(desktop)
                .tablet(tablet)
                .unknown(unknown)
                .mobilePercentage(total > 0 ? (mobile * 100.0 / total) : 0.0)
                .desktopPercentage(total > 0 ? (desktop * 100.0 / total) : 0.0)
                .tabletPercentage(total > 0 ? (tablet * 100.0 / total) : 0.0)
                .build();
    }

    private List<AnalyticsResponse.BrowserStats> getTopBrowsers(List<UrlClick> clicks) {
        long totalClicks = clicks.size();

        Map<String, Long> browserMap = clicks.stream()
                .filter(click -> click.getBrowser() != null)
                .collect(Collectors.groupingBy(
                        UrlClick::getBrowser,
                        Collectors.counting()
                ));

        return browserMap.entrySet().stream()
                .map(entry -> AnalyticsResponse.BrowserStats.builder()
                        .browser(entry.getKey())
                        .clicks(entry.getValue())
                        .percentage(totalClicks > 0 ? (entry.getValue() * 100.0 / totalClicks) : 0.0)
                        .build())
                .sorted((a, b) -> Long.compare(b.getClicks(), a.getClicks()))
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<AnalyticsResponse.ReferrerStats> getTopReferrers(List<UrlClick> clicks) {
        long totalClicks = clicks.size();

        Map<String, Long> referrerMap = clicks.stream()
                .filter(click -> click.getReferrer() != null && !click.getReferrer().isEmpty())
                .collect(Collectors.groupingBy(
                        UrlClick::getReferrer,
                        Collectors.counting()
                ));

        return referrerMap.entrySet().stream()
                .map(entry -> AnalyticsResponse.ReferrerStats.builder()
                        .referrer(entry.getKey())
                        .clicks(entry.getValue())
                        .percentage(totalClicks > 0 ? (entry.getValue() * 100.0 / totalClicks) : 0.0)
                        .build())
                .sorted((a, b) -> Long.compare(b.getClicks(), a.getClicks()))
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<AnalyticsResponse.ClickEvent> getRecentClicks(List<UrlClick> clicks, int limit) {
        return clicks.stream()
                .limit(limit)
                .map(click -> AnalyticsResponse.ClickEvent.builder()
                        .timestamp(click.getClickedAt())
                        .country(click.getCountry())
                        .city(click.getCity())
                        .deviceType(click.getDeviceType())
                        .browser(click.getBrowser())
                        .referrer(click.getReferrer())
                        .build())
                .collect(Collectors.toList());
    }

    public AnalyticsResponse getAnalyticsByDateRange(String shortCode, LocalDateTime start, LocalDateTime end) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short Url not found " + shortCode));

        List<UrlClick> allClicks = urlClickRepository.findByUrlOrderByClickedAtDesc(url);
        List<UrlClick> filteredClicks = allClicks.stream()
                .filter(click -> !click.getClickedAt().isBefore(start) && !click.getClickedAt().isAfter(end))
                .collect(Collectors.toList());

        return AnalyticsResponse.builder()
                .shortCode(url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .totalClicks((long) filteredClicks.size())
                .createdAt(url.getCreatedAt())
                .clicksByDate(getClicksByDate(filteredClicks))
                .clicksByHour(getClicksByHour(filteredClicks))
                .topCountries(getTopCountries(filteredClicks))
                .topCities(getTopCities(filteredClicks))
                .deviceStats(getDeviceStats(filteredClicks))
                .topBrowsers(getTopBrowsers(filteredClicks))
                .topReferrers(getTopReferrers(filteredClicks))
                .recentClicks(getRecentClicks(filteredClicks, 10))
                .build();
    }
}
