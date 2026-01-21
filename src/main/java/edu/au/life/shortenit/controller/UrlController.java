package edu.au.life.shortenit.controller;

import edu.au.life.shortenit.dto.UrlResponse;
import edu.au.life.shortenit.dto.UrlShortenRequest;
import edu.au.life.shortenit.dto.UrlUpdateRequest;
import edu.au.life.shortenit.dto.UrlWithAnalyticsResponse;
import edu.au.life.shortenit.entity.User;
import edu.au.life.shortenit.service.UrlService;
import edu.au.life.shortenit.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UrlResponse> createShortUrl(@Valid @RequestBody UrlShortenRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();
        UrlResponse response = urlService.shortenUrl(request, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UrlResponse>> getUserUrls() {
        User currentUser = SecurityUtils.getCurrentUser();
        List<UrlResponse> response = urlService.getAllUrls(currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analytics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<UrlWithAnalyticsResponse>> getUserUrlsWithAnalytics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {

        User currentUser = SecurityUtils.getCurrentUser();

        Sort.Direction sortDirection = direction.equalsIgnoreCase("ASC") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<UrlWithAnalyticsResponse> response = urlService.getAllUrlsWithAnalytics(currentUser, pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{code}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UrlResponse> getUrlByCode(@PathVariable String code) {
        User currentUser = SecurityUtils.getCurrentUser();
        UrlResponse response = urlService.getUrlInfo(code, currentUser);  // PHASE 3: UPDATED
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{code}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UrlResponse> updateUrl(
            @PathVariable String code,
            @Valid @RequestBody UrlUpdateRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();
        UrlResponse response = urlService.updateUrl(code, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteUrl(@PathVariable String code) {
        User currentUser = SecurityUtils.getCurrentUser();
        urlService.deleteUrl(code, currentUser);  // PHASE 3: UPDATED
        return ResponseEntity.ok().build();
    }
}