package ledgerline.auth;

import ledgerline.audit.AuditAction;
import ledgerline.audit.AuditOutcome;
import ledgerline.audit.AuditService;
import ledgerline.common.ApiException;
import ledgerline.user.Role;
import ledgerline.user.User;
import ledgerline.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    static final int MAX_FAILED_LOGINS = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    public record LoginResult(String token, Instant expiresAt, User user) {
    }

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final SessionService sessions;
    private final AuditService audit;
    /** Used to spend the same time hashing when the username does not exist (stops user-guessing by timing). */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, SessionService sessions, AuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.sessions = sessions;
        this.audit = audit;
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    /** Public registration always creates a CUSTOMER. Admins can never be created through the API. */
    @Transactional
    public User register(String username, String password) {
        String name = normalise(username);
        if (users.findByUsername(name).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "Username is already taken");
        }
        User user = users.save(new User(name, encoder.encode(password), Role.CUSTOMER));
        audit.record(AuditAction.REGISTER, AuditOutcome.SUCCESS, user.getId(), name, "USER", String.valueOf(user.getId()), null);
        return user;
    }

    /**
     * noRollbackFor: when the login fails we still want the failed-attempt counter and the audit row saved.
     * Every failure returns the same message so attackers learn nothing about which usernames exist.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public LoginResult login(String username, String password) {
        String name = normalise(username);
        Instant now = Instant.now();
        User user = users.findByUsername(name).orElse(null);

        if (user == null) {
            encoder.matches(password, dummyHash);
            audit.record(AuditAction.LOGIN_FAILURE, AuditOutcome.FAILED, null, name, "USER", null, "Unknown username");
            throw invalid();
        }
        if (user.isLocked(now)) {
            audit.record(AuditAction.LOGIN_FAILURE, AuditOutcome.DENIED, user.getId(), name, "USER",
                    String.valueOf(user.getId()), "Account temporarily locked");
            throw invalid();
        }
        if (!encoder.matches(password, user.getPasswordHash())) {
            user.registerFailedLogin(MAX_FAILED_LOGINS, LOCK_DURATION, now);
            audit.record(AuditAction.LOGIN_FAILURE, AuditOutcome.FAILED, user.getId(), name, "USER",
                    String.valueOf(user.getId()), "Wrong password");
            throw invalid();
        }

        user.resetFailedLogins();
        SessionService.IssuedSession session = sessions.create(user);
        audit.record(AuditAction.LOGIN_SUCCESS, AuditOutcome.SUCCESS, user.getId(), name, "USER",
                String.valueOf(user.getId()), null);
        return new LoginResult(session.token(), session.expiresAt(), user);
    }

    private static ApiException invalid() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username or password");
    }

    private static String normalise(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
