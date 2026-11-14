package edu.csuf.sis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A signed-in session. Only a SHA-256 hash of the bearer token is stored, so a copy of
 * the database cannot be used to impersonate anyone. Sessions live in the database
 * (not server memory) so any app server behind the load balancer can check them.
 */
@Entity
@Table(name = "user_session")
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private UserAccount account;

    @Column(nullable = false)
    private Instant expiresAt;

    protected UserSession() {
    }

    public UserSession(String tokenHash, UserAccount account, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.account = account;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public Long getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UserAccount getAccount() {
        return account;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
