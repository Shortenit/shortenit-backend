package edu.au.life.shortenit.repository;

import edu.au.life.shortenit.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByMicrosoftId(String microsoftId);
    boolean existsByEmail(String mail);
    boolean existsByMicrosoftId(String microsoft);
}
