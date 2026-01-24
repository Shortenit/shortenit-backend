package edu.au.life.shortenit.repository;

import edu.au.life.shortenit.entity.Url;
import edu.au.life.shortenit.entity.UrlClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface UrlClickRepository extends JpaRepository<UrlClick, Long> {
    List<UrlClick> findByUrl(Url url);
    List<UrlClick> findByUrlOrderByClickedAtDesc(Url url);
    List<UrlClick> findByUrlCode(String code);

    @Query("SELECT COUNT(c) FROM UrlClick c WHERE c.url.id = :urlId")
    int countByUrlId(@Param("urlId") Long urlId);

    @Query("SELECT MAX(c.clickedAt) FROM UrlClick c WHERE c.url.id = :urlId")
    LocalDateTime findLastClickTimeByUrlId(@Param("urlId") Long urlId);

    @Query("SELECT c.country FROM UrlClick c WHERE c.url.id = :urlId " +
            "GROUP BY c.country ORDER BY COUNT(c) DESC")
    List<String> findTopCountryByUrlId(@Param("urlId") Long urlId);

    @Query("SELECT c.city FROM UrlClick c WHERE c.url.id = :urlId " +
            "GROUP BY c.city ORDER BY COUNT(c) DESC")
    List<String> findTopCityByUrlId(@Param("urlId") Long urlId);

    @Query("SELECT c.browser FROM UrlClick c WHERE c.url.id = :urlId " +
            "GROUP BY c.browser ORDER BY COUNT(c) DESC")
    List<String> findTopBrowserByUrlId(@Param("urlId") Long urlId);

    @Query("SELECT c.deviceType FROM UrlClick c WHERE c.url.id = :urlId " +
            "GROUP BY c.deviceType ORDER BY COUNT(c) DESC")
    List<String> findTopDeviceByUrlId(@Param("urlId") Long urlId);

    // Efficient batch query - get analytics for multiple URLs at once
    @Query("SELECT c.url.id as urlId, COUNT(c) as clickCount, " +
            "MAX(c.clickedAt) as lastClick " +
            "FROM UrlClick c WHERE c.url.id IN :urlIds " +
            "GROUP BY c.url.id")
    List<Map<String, Object>> findAnalyticsSummaryForUrls(@Param("urlIds") List<Long> urlIds);
}
