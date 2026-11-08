package ledgerline.auth;

import jakarta.persistence.*;
import ledgerline.user.User;

import java.time.Instant;

/** A login session. Only the SHA-256 hash of the token is stored, so a database leak does not leak usable tokens. */
@Entity
@Table(name = "auth_sessions")
public class AuthSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    protected AuthSession() {
    }

    public AuthSession(String tokenHash, User user, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    public boolean isActive(Instant now) {
        return !revoked && expiresAt.isAfter(now);
    }

    public void revoke() {
        this.revoked = true;
    }

    public User getUser() { return user; }
    public Instant getExpiresAt() { return expiresAt; }
}
