package edu.au.life.shortenit.repository;

import edu.au.life.shortenit.entity.Url;
import edu.au.life.shortenit.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByCode(String code);

    boolean existsByCode(String code);

    Page<Url> findAll(Pageable pageable);

    @Query("SELECT u FROM Url u ORDER BY u.createdAt DESC")
    Page<Url> findAllOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT u FROM Url u WHERE u.isActive = true ORDER BY u.createdAt DESC")
    Page<Url> findAllActiveUrls(Pageable pageable);

    @Query("SELECT COUNT(u) FROM Url u WHERE u.isActive = true")
    long countActiveUrls();

    List<Url> findByUser(User user);

    Page<Url> findByUser(User user, Pageable pageable);

    @Query("SELECT u FROM Url u WHERE u.user = :user ORDER BY u.createdAt DESC")
    Page<Url> findByUserOrderByCreatedAtDesc(@Param("user") User user, Pageable pageable);

    @Query("SELECT u FROM Url u WHERE u.user = :user AND u.isActive = true ORDER BY u.createdAt DESC")
    Page<Url> findActiveUrlsByUser(@Param("user") User user, Pageable pageable);

    long countByUser(User user);

    @Query("SELECT COUNT(u) FROM Url u WHERE u.user = :user AND u.isActive = true")
    long countActiveUrlsByUser(@Param("user") User user);

    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1 WHERE u.id = :urlId")
    void incrementClickCount(@Param("urlId") Long urlId);
}