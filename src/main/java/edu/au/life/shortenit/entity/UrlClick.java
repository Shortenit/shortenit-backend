package edu.au.life.shortenit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="url_clicks")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UrlClick {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="url_id", nullable = false)
    private Url url;

    @Column(nullable = false)
    private LocalDateTime clickedAt;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String city;

    @Column(length = 500)
    private String userAgent;

    @Column(length = 50)
    private String deviceType;

    @Column(length = 100)
    private String browser;

    @Column(length = 100)
    private String operatingSystem;

    @Column(length = 500)
    private String referrer;

    @PrePersist
    protected void onCreate() {
        clickedAt =  LocalDateTime.now();
    }

}
