package edu.au.life.shortenit.repository;

import edu.au.life.shortenit.entity.Url;
import edu.au.life.shortenit.entity.UrlClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UrlClickRepository extends JpaRepository<UrlClick, Long> {
    List<UrlClick> findByUrl(Url url);
    List<UrlClick> findByUrlOrderByClickedAtDesc(Url url);

    @Query("SELECT COUNT(c) FROM UrlClick c WHERE c.url = :url")
    Long countByUrl(@Param("url") Url url);

    @Query("SELECT COUNT(c) FROM UrlClick c WHERE c.url = :url AND c.clickedAt BETWEEN :start AND :end")
    Long countByUrlAndClickedAtBetween(
            @Param("url") Url url,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT c.country, COUNT(c) FROM UrlClick c WHERE c.url = :url GROUP BY c.country ORDER BY COUNT(c) DESC")
    List<Object[]> countByCountry(@Param("url") Url url);

    @Query("SELECT c.city, COUNT(c) FROM UrlClick c WHERE c.url = :url GROUP BY c.city ORDER BY COUNT(c) DESC")
    List<Object[]> countByCity(@Param("url") Url url);

    @Query("SELECT c.deviceType, COUNT(c) FROM UrlClick c WHERE c.url = :url GROUP BY c.deviceType")
    List<Object[]> countByDeviceType(@Param("url") Url url);

    @Query("SELECT c.browser, COUNT(c) FROM UrlClick c WHERE c.url = :url GROUP BY c.browser ORDER BY COUNT(c) DESC")
    List<Object[]> countByBrowser(@Param("url") Url url);

    @Query("SELECT c.referrer, COUNT(c) FROM UrlClick c WHERE c.url = :url AND c.referrer IS NOT NULL GROUP BY c.referrer ORDER BY COUNT(c) DESC")
    List<Object[]> countByReferrer(@Param("url") Url url);
}
