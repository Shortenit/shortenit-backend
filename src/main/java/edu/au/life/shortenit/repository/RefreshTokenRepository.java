package edu.au.life.shortenit.repository;

import edu.au.life.shortenit.entity.RefreshToken;
import edu.au.life.shortenit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.user = :user")
    void deleteByUser(@Param("user") User user); // when user logout, it will delete all refresh token for user

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now); // delete all expired token

    @Query("SELECT COUNT(r) > 0 FROM RefreshToken r WHERE r.user = :user AND r.expiresAt > :now")
    boolean hasActiveToken(@Param("user") User user, @Param("now") LocalDateTime now); // check user have any active token
}
