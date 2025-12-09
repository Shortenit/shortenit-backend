package edu.au.life.shortenit.service;

import edu.au.life.shortenit.dto.UrlResponse;
import edu.au.life.shortenit.dto.UrlShortenRequest;
import edu.au.life.shortenit.exception.CustomAliasAlreadyExistsException;
import edu.au.life.shortenit.exception.UrlNotFoundException;
import edu.au.life.shortenit.entity.Url;
import edu.au.life.shortenit.entity.UrlClick;
import edu.au.life.shortenit.repository.UrlClickRepository;
import edu.au.life.shortenit.repository.UrlRepository;
import edu.au.life.shortenit.util.UserAgentParser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final UrlClickRepository urlClickRepository;
    private final GeoIpService geoIpService;
    private final UserAgentParser userAgentParser;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 7;
    private static final SecureRandom random = new SecureRandom();

    @Transactional
    public UrlResponse shortenUrl(UrlShortenRequest request) {
        Url url = new Url();
        url.setOriginalUrl(request.getOriginalUrl());

        // Handle custom alias
        if (request.getCustomAlias() != null && !request.getCustomAlias().isEmpty()) {
            if (urlRepository.existsByCustomAlias(request.getCustomAlias())) {
                throw new CustomAliasAlreadyExistsException("Custom alias already exists: " + request.getCustomAlias());
            }
            url.setCustomAlias(request.getCustomAlias());
            url.setShortCode(request.getCustomAlias());
        } else {
            url.setShortCode(generateUniqueShortCode());
        }

        // Handle expiration
        if (request.getExpirationDays() != null && request.getExpirationDays() > 0) {
            url.setExpiresAt(LocalDateTime.now().plusDays(request.getExpirationDays()));
        }

        Url savedUrl = urlRepository.save(url);

        return convertToResponse(savedUrl);
    }

    @Transactional
    public String getOriginalUrl(String shortCode, HttpServletRequest request) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));

        // Check if URL is active
        if (!url.getIsActive()) {
            throw new UrlNotFoundException("This URL has been deactivated");
        }

        // Check if URL has expired
        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UrlNotFoundException("This URL has expired");
        }

        // Track the click with analytics
        trackClick(url, request);

        // Increment click count
        url.setClickCount(url.getClickCount() + 1);
        urlRepository.save(url);

        return url.getOriginalUrl();
    }

    private void trackClick(Url url, HttpServletRequest request) {
        UrlClick click = new UrlClick();
        click.setUrl(url);

        // Get IP address
        String ipAddress = getClientIpAddress(request);
        click.setIpAddress(ipAddress);

        // Get geographical data
        GeoIpService.GeoLocation geoLocation = geoIpService.getGeoLocation(ipAddress);
        click.setCountry(geoLocation.getCountry());
        click.setCity(geoLocation.getCity());

        // Get user agent
        String userAgent = request.getHeader("User-Agent");
        click.setUserAgent(userAgent);

        // Parse user agent for device/browser info
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

    public UrlResponse getUrlInfo(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));

        return convertToResponse(url);
    }

    public List<UrlResponse> getAllUrls() {
        return urlRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUrl(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortCode));

        urlRepository.delete(url);
    }

    private String generateUniqueShortCode() {
        String shortCode;
        do {
            shortCode = generateRandomString(SHORT_CODE_LENGTH);
        } while (urlRepository.existsByShortCode(shortCode));

        return shortCode;
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
                .shortCode(url.getShortCode())
                .shortUrl(baseUrl + "/" + url.getShortCode())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .clickCount(url.getClickCount())
                .customAlias(url.getCustomAlias())
                .build();
    }
}