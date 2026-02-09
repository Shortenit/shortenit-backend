package edu.au.life.shortenit.controller;

import edu.au.life.shortenit.dto.AnalyticsResponse;
import edu.au.life.shortenit.entity.User;
import edu.au.life.shortenit.service.AnalyticsService;
import edu.au.life.shortenit.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllAnalytics(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        User currentUser = SecurityUtils.getCurrentUser();

        if (page != null && size != null) {
            int pageIndex = Math.max(0, page - 1);
            Pageable pageable = PageRequest.of(pageIndex, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<AnalyticsResponse> response = analyticsService.getAllAnalyticsPaginated(currentUser, pageable);
            return ResponseEntity.ok(response);
        }

        List<AnalyticsResponse> response = analyticsService.getAllAnalytics(currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shortCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnalyticsResponse> getAnalytics(@PathVariable String shortCode) {
        User currentUser = SecurityUtils.getCurrentUser();
        AnalyticsResponse analytics = analyticsService.getAnalytics(shortCode, currentUser);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/{shortCode}/range")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnalyticsResponse> getAnalyticsByDateRange(
            @PathVariable String shortCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        User currentUser = SecurityUtils.getCurrentUser();
        AnalyticsResponse analytics = analyticsService.getAnalyticsByDateRange(shortCode, start, end, currentUser);
        return ResponseEntity.ok(analytics);
    }
}