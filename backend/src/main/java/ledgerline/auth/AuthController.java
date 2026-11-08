package ledgerline.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ledgerline.audit.AuditAction;
import ledgerline.audit.AuditOutcome;
import ledgerline.audit.AuditService;
import ledgerline.security.AuthenticatedUser;
import ledgerline.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record RegisterRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9_.-]{3,30}$",
                    message = "3-30 characters: letters, digits, dot, underscore, dash") String username,
            @NotBlank @Size(min = 10, max = 64, message = "Password must be 10-64 characters") String password) {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank @Size(max = 64) String password) {
    }

    public record UserResponse(Long id, String username, String role) {
        static UserResponse of(User u) {
            return new UserResponse(u.getId(), u.getUsername(), u.getRole().name());
        }
    }

    public record LoginResponse(String token, String expiresAt, UserResponse user) {
    }

    private final AuthService authService;
    private final SessionService sessionService;
    private final AuditService audit;

    public AuthController(AuthService authService, SessionService sessionService, AuditService audit) {
        this.authService = authService;
        this.sessionService = sessionService;
        this.audit = audit;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.username(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.of(user));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.username(), request.password());
        return new LoginResponse(result.token(), result.expiresAt().toString(), UserResponse.of(result.user()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUser user, HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            sessionService.revoke(header.substring(7).trim());
        }
        audit.record(AuditAction.LOGOUT, AuditOutcome.SUCCESS, user.id(), user.username(), "USER",
                String.valueOf(user.id()), null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return new UserResponse(user.id(), user.username(), user.role().name());
    }
}
