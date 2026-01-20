package edu.au.life.shortenit.repository;

import edu.au.life.shortenit.entity.ApiKey;
import edu.au.life.shortenit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyHash(String keyHash);

    List<ApiKey> findByUser(User user);

    List<ApiKey> findByUserOrderByCreatedAtDesc(User user);

    @Query("SELECT k FROM ApiKey k WHERE k.user = :user AND " +
            "(k.expiresAt IS NULL OR k.expiresAt > :now)")
    List<ApiKey> findActiveByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE ApiKey k SET k.lastUsedAt = :now WHERE k.id = :id")
    void updateLastUsedAt(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM ApiKey k WHERE k.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);

    @Query("SELECT k FROM ApiKey k JOIN FETCH k.user")
    List<ApiKey> findAllWithUser();

}
