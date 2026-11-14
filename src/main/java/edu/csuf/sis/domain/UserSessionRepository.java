package edu.csuf.sis.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    @EntityGraph(attributePaths = "account")
    Optional<UserSession> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from UserSession s where s.account.id = :accountId")
    void deleteAllForAccount(Long accountId);

    @Modifying
    @Query("delete from UserSession s where s.expiresAt <= :now")
    void deleteExpired(Instant now);
}
