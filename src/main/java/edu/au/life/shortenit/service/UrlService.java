package edu.au.life.shortenit.service;

import edu.au.life.shortenit.dto.GeoLocation;
import edu.au.life.shortenit.dto.UrlResponse;
import edu.au.life.shortenit.dto.UrlShortenRequest;
import edu.au.life.shortenit.dto.UrlUpdateRequest;
import edu.au.life.shortenit.dto.UrlWithAnalyticsResponse;
import edu.au.life.shortenit.exception.CustomAliasAlreadyExistsException;
import edu.au.life.shortenit.exception.UrlNotFoundException;
import edu.au.life.shortenit.entity.Url;
import edu.au.life.shortenit.entity.UrlClick;
import edu.au.life.shortenit.entity.User;
import edu.au.life.shortenit.repository.UrlClickRepository;
import edu.au.life.shortenit.repository.UrlRepository;
import edu.au.life.shortenit.util.UserAgentParser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final UrlClickRepository urlClickRepository;
    private final LocalGeoIpService localGeoIpService;
    private final UserAgentParser userAgentParser;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 7;
    private static final SecureRandom random = new SecureRandom();

    @Transactional
    public UrlResponse shortenUrl(UrlShortenRequest request, User user) {
        Url url = new Url();
        url.setOriginalUrl(request.getOriginalUrl());
        url.setTitle(request.getTitle());
        url.setUser(user);

        if (request.getCode() != null && !request.getCode().isBlank()) {
            String code = request.getCode();

            if (urlRepository.existsByCode(code)) {
                throw new CustomAliasAlreadyExistsException("Code already exists: " + code);
            }

            url.setCode(code);
            url.setCodeType(Url.CodeType.CUSTOM);
        } else {
            url.setCode(generateUniqueCode());
            url.setCodeType(Url.CodeType.AUTO);
        }

        if (request.getExpirationDays() != null && request.getExpirationDays() > 0) {
            url.setExpiresAt(LocalDateTime.now().plusDays(request.getExpirationDays()));
        }

        Url saved = urlRepository.save(url);
        return convertToResponse(saved);
    }


    @Transactional
    public String getOriginalUrl(String code, HttpServletRequest request) {
        Url url = urlRepository.findByCode(code)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + code));

        if (!url.getIsActive()) throw new UrlNotFoundException("This URL has been deactivated");
        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new UrlNotFoundException("This URL has expired");

        trackClick(url, request);
        urlRepository.incrementClickCount(url.getId());
        return url.getOriginalUrl();
    }


    private void trackClick(Url url, HttpServletRequest request) {
        UrlClick click = new UrlClick();
        click.setUrl(url);

        String ipAddress = getClientIpAddress(request);
        click.setIpAddress(ipAddress);

        GeoLocation geoLocation = localGeoIpService.getLocation(ipAddress);
        click.setCountry(geoLocation.getCountry());
        click.setCity(geoLocation.getCity());

        String userAgent = request.getHeader("User-Agent");
        click.setUserAgent(userAgent);

        click.setDeviceType(userAgentParser.getDeviceType(userAgent));
        click.setBrowser(userAgentParser.getBrowser(userAgent));
        click.setOperatingSystem(userAgentParser.getOperatingSystem(userAgent));

        // Get referrer
        String referrer = request.getHeader("Referer");
        click.setReferrer(referrer);

        urlClickRepository.save(click);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    public UrlResponse getUrlInfo(String shortCode, User user) {
        Url url = urlRepository.findByCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));

        if (!url.getUser().getId().equals(user.getId()) &&
                !user.getRole().equals(User.Role.ADMIN)) {
            throw new UrlNotFoundException("Short URL not found: " + shortCode);
        }

        return convertToResponse(url);
    }

    public List<UrlResponse> getAllUrls(User user) {
        return urlRepository.findByUser(user).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUrl(String shortCode, User user) {
        Url url = urlRepository.findByCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));

        if (!url.getUser().getId().equals(user.getId()) &&
                !user.getRole().equals(User.Role.ADMIN)) {
            throw new UrlNotFoundException("Short URL not found: " + shortCode);
        }

        urlRepository.delete(url);
    }

    @Transactional
    public UrlResponse updateUrl(String code, UrlUpdateRequest request, User user) {
        Url url = urlRepository.findByCode(code)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + code));

        // auth check same

        if (request.getTitle() != null) url.setTitle(request.getTitle());

        if (request.getCode() != null && !request.getCode().isBlank()) {
            String newCode = request.getCode();
            if (!newCode.equals(url.getCode()) && urlRepository.existsByCode(newCode)) {
                throw new CustomAliasAlreadyExistsException("Code already exists: " + newCode);
            }
            url.setCode(newCode);
            url.setCodeType(Url.CodeType.CUSTOM);
        }

        if (Boolean.TRUE.equals(request.getClearExpiration())) {
            url.setExpiresAt(null);
        } else if (request.getExpirationDays() != null && request.getExpirationDays() > 0) {
            url.setExpiresAt(LocalDateTime.now().plusDays(request.getExpirationDays()));
        }

        return convertToResponse(urlRepository.save(url));
    }


    private String generateUniqueCode() {
        String code;
        do {
            code = generateRandomString(SHORT_CODE_LENGTH);
        } while (urlRepository.existsByCode(code));
        return code;
    }


    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private UrlResponse convertToResponse(Url url) {
        return UrlResponse.builder()
                .id(url.getId())
                .originalUrl(url.getOriginalUrl())
                .code(url.getCode())
                .shortUrl(baseUrl + "/s/" + url.getCode())
                .title(url.getTitle())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .clickCount(url.getClickCount())
                .owner(UrlResponse.UserInfo.builder()
                        .id(url.getUser().getId())
                        .name(url.getUser().getName())
                        .email(url.getUser().getEmail())
                        .build())
                .build();
    }


    @Transactional(readOnly = true)
    public Page<UrlWithAnalyticsResponse> getAllUrlsWithAnalytics(User user, Pageable pageable) {
        Page<Url> urlPage = urlRepository.findByUser(user, pageable);

        return urlPage.map(url -> {
            UrlWithAnalyticsResponse.AnalyticsSummary summary = getAnalyticsSummary(url);

            return UrlWithAnalyticsResponse.builder()
                    .code(url.getCode())
                    .originalUrl(url.getOriginalUrl())
                    .title(url.getTitle())
                    .clickCount(url.getClickCount())
                    .createdAt(url.getCreatedAt())
                    .expiresAt(url.getExpiresAt())
                    .isExpired(url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now()))
                    .ownerName(url.getUser().getName())
                    .ownerEmail(url.getUser().getEmail())
                    .analyticsSummary(summary)
                    .build();
        });
    }

    @Transactional(readOnly = true)
    public Page<UrlWithAnalyticsResponse> getRecentUrlsWithAnalytics(User user, Pageable pageable) {
        return getAllUrlsWithAnalytics(user, pageable);
    }

    private UrlWithAnalyticsResponse.AnalyticsSummary getAnalyticsSummary(Url url) {
        List<UrlClick> clicks = urlClickRepository.findByUrlOrderByClickedAtDesc(url);

        if (clicks.isEmpty()) {
            return UrlWithAnalyticsResponse.AnalyticsSummary.builder()
                    .totalClicks(0L)
                    .lastClickedAt(null)
                    .topCountry(null)
                    .topCountryClicks(0L)
                    .topDeviceType(null)
                    .topDeviceClicks(0L)
                    .clicksToday(0L)
                    .clicksThisWeek(0L)
                    .build();
        }

        LocalDateTime lastClickedAt = clicks.get(0).getClickedAt();

        Map<String, Long> countryMap = clicks.stream()
                .filter(click -> click.getCountry() != null)
                .collect(Collectors.groupingBy(UrlClick::getCountry, Collectors.counting()));

        Map.Entry<String, Long> topCountryEntry = countryMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        String topCountry = topCountryEntry != null ? topCountryEntry.getKey() : null;
        Long topCountryClicks = topCountryEntry != null ? topCountryEntry.getValue() : 0L;

        Map<String, Long> deviceMap = clicks.stream()
                .filter(click -> click.getDeviceType() != null)
                .collect(Collectors.groupingBy(UrlClick::getDeviceType, Collectors.counting()));

        Map.Entry<String, Long> topDeviceEntry = deviceMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        String topDeviceType = topDeviceEntry != null ? topDeviceEntry.getKey() : null;
        Long topDeviceClicks = topDeviceEntry != null ? topDeviceEntry.getValue() : 0L;

        LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        Long clicksToday = clicks.stream()
                .filter(click -> click.getClickedAt().isAfter(startOfToday))
                .count();

        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        Long clicksThisWeek = clicks.stream()
                .filter(click -> click.getClickedAt().isAfter(oneWeekAgo))
                .count();

        return UrlWithAnalyticsResponse.AnalyticsSummary.builder()
                .totalClicks((long) clicks.size())
                .lastClickedAt(lastClickedAt)
                .topCountry(topCountry)
                .topCountryClicks(topCountryClicks)
                .topDeviceType(topDeviceType)
                .topDeviceClicks(topDeviceClicks)
                .clicksToday(clicksToday)
                .clicksThisWeek(clicksThisWeek)
                .build();
    }
}