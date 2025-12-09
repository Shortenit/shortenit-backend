package edu.au.life.shortenit.controller;

import edu.au.life.shortenit.dto.AnalyticsResponse;
import edu.au.life.shortenit.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Get complete analytics for a shortened URL
     * GET /api/analytics/{shortCode}
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<AnalyticsResponse> getAnalytics(@PathVariable String shortCode) {
        AnalyticsResponse analytics = analyticsService.getAnalytics(shortCode);
        return ResponseEntity.ok(analytics);
    }

    /**
     * Get analytics for a shortened URL within a date range
     * GET /api/analytics/{shortCode}/range?start=2024-01-01T00:00:00&end=2024-01-31T23:59:59
     */
    @GetMapping("/{shortCode}/range")
    public ResponseEntity<AnalyticsResponse> getAnalyticsByDateRange(
            @PathVariable String shortCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        AnalyticsResponse analytics = analyticsService.getAnalyticsByDateRange(shortCode, start, end);
        return ResponseEntity.ok(analytics);
    }
}