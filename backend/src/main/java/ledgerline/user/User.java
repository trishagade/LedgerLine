
package ledgerline.user;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "app_users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 254)
    private String username;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "email", unique = true, length = 254)
    private String email;

    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;

    /** BCrypt hash. The real password is never stored. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "failed_logins", nullable = false)
    private int failedLogins;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected User() {
    }

    // Existing constructor: preserves admin bootstrap compatibility.
    public User(String username, String passwordHash, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    // Constructor for new customer registration.
    public User(String email, String fullName, String mobileNumber,
                String passwordHash, Role role) {
        this.username = email;
        this.email = email;
        this.fullName = fullName;
        this.mobileNumber = mobileNumber;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void registerFailedLogin(int maxFailures, Duration lockFor, Instant now) {
        failedLogins++;
        if (failedLogins >= maxFailures) {
            lockedUntil = now.plus(lockFor);
            failedLogins = 0;
        }
    }

    public void resetFailedLogins() {
        failedLogins = 0;
        lockedUntil = null;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getMobileNumber() { return mobileNumber; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
}
