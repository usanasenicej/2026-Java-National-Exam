package com.wasac.ne.repository;

import com.wasac.ne.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

    boolean existsByToken(String token);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
