
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
    private final String dummyHash;

    public AuthService(
            UserRepository users,
            PasswordEncoder encoder,
            SessionService sessions,
            AuditService audit
    ) {
        this.users = users;
        this.encoder = encoder;
        this.sessions = sessions;
        this.audit = audit;
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    /** Public registration always creates a CUSTOMER. */
    @Transactional
    public User register(
            String email,
            String fullName,
            String mobileNumber,
            String password
    ) {
        String normalizedEmail = normalise(email);

        if (users.findByEmail(normalizedEmail).isPresent()
                || users.findByUsername(normalizedEmail).isPresent()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EMAIL_TAKEN",
                    "Email address is already registered"
            );
        }

        String name = fullName.trim();
        String mobile = (mobileNumber == null || mobileNumber.isBlank())
                ? null : mobileNumber.trim();

        User user = users.save(new User(
                normalizedEmail,
                name,
                mobile,
                encoder.encode(password),
                Role.CUSTOMER
        ));

        audit.record(
                AuditAction.REGISTER,
                AuditOutcome.SUCCESS,
                user.getId(),
                normalizedEmail,
                "USER",
                String.valueOf(user.getId()),
                null
        );

        return user;
    }

    /**
     * Supports email login for new customers and username login
     * for existing accounts such as the admin.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public LoginResult login(String username, String password) {
        String identifier = normalise(username);
        Instant now = Instant.now();

        User user = users.findByUsername(identifier)
                .or(() -> users.findByEmail(identifier))
                .orElse(null);

        if (user == null) {
            encoder.matches(password, dummyHash);
            audit.record(
                    AuditAction.LOGIN_FAILURE,
                    AuditOutcome.FAILED,
                    null,
                    identifier,
                    "USER",
                    null,
                    "Unknown username or email"
            );
            throw invalid();
        }

        if (user.isLocked(now)) {
            audit.record(
                    AuditAction.LOGIN_FAILURE,
                    AuditOutcome.DENIED,
                    user.getId(),
                    identifier,
                    "USER",
                    String.valueOf(user.getId()),
                    "Account temporarily locked"
            );
            throw invalid();
        }

        if (!encoder.matches(password, user.getPasswordHash())) {
            user.registerFailedLogin(MAX_FAILED_LOGINS, LOCK_DURATION, now);

            audit.record(
                    AuditAction.LOGIN_FAILURE,
                    AuditOutcome.FAILED,
                    user.getId(),
                    identifier,
                    "USER",
                    String.valueOf(user.getId()),
                    "Wrong password"
            );

            throw invalid();
        }

        user.resetFailedLogins();

        SessionService.IssuedSession session = sessions.create(user);

        audit.record(
                AuditAction.LOGIN_SUCCESS,
                AuditOutcome.SUCCESS,
                user.getId(),
                user.getUsername(),
                "USER",
                String.valueOf(user.getId()),
                null
        );

        return new LoginResult(
                session.token(),
                session.expiresAt(),
                user
        );
    }

    private static ApiException invalid() {
        return new ApiException(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                "Invalid username/email or password"
        );
    }

    private static String normalise(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }
}
