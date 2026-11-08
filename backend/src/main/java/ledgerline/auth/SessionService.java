package ledgerline.auth;

import ledgerline.common.Hashing;
import ledgerline.security.AuthenticatedUser;
import ledgerline.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class SessionService {

    public record IssuedSession(String token, Instant expiresAt) {
    }

    private final SessionRepository sessions;
    private final long sessionHours;

    public SessionService(SessionRepository sessions,
                          @Value("${ledgerline.security.session-hours:8}") long sessionHours) {
        this.sessions = sessions;
        this.sessionHours = sessionHours;
    }

    /** Creates a random token, stores only its hash, and returns the raw token once. */
    @Transactional
    public IssuedSession create(User user) {
        String token = Hashing.newToken();
        Instant expires = Instant.now().plus(Duration.ofHours(sessionHours));
        sessions.save(new AuthSession(Hashing.sha256Hex(token), user, expires));
        return new IssuedSession(token, expires);
    }

    /** Looks the token up server-side. Unknown, expired or revoked tokens give an empty result. */
    @Transactional(readOnly = true)
    public Optional<AuthenticatedUser> authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 200) return Optional.empty();
        return sessions.findByTokenHash(Hashing.sha256Hex(rawToken))
                .filter(s -> s.isActive(Instant.now()))
                .map(s -> new AuthenticatedUser(s.getUser().getId(), s.getUser().getUsername(), s.getUser().getRole()));
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null) return;
        sessions.findByTokenHash(Hashing.sha256Hex(rawToken)).ifPresent(AuthSession::revoke);
    }
}
