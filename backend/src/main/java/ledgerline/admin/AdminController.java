package ledgerline.admin;

import ledgerline.account.AccountController.AccountResponse;
import ledgerline.account.AccountService;
import ledgerline.audit.*;
import ledgerline.security.AuthenticatedUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Every route here is ADMIN-only: enforced in SecurityConfig (/api/admin/** requires ROLE_ADMIN). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    public record AuditLogView(Long id, String createdAt, Long actorUserId, String actorUsername, String action,
                               String outcome, String targetType, String targetId, String details, String ipAddress) {
        static AuditLogView of(AuditLog a) {
            return new AuditLogView(a.getId(), a.getCreatedAt().toString(), a.getActorUserId(), a.getActorUsername(),
                    a.getAction().name(), a.getOutcome().name(), a.getTargetType(), a.getTargetId(),
                    a.getDetails(), a.getIpAddress());
        }
    }

    private final AccountService accountService;
    private final AuditLogRepository auditLogs;
    private final AuditService audit;

    public AdminController(AccountService accountService, AuditLogRepository auditLogs, AuditService audit) {
        this.accountService = accountService;
        this.auditLogs = auditLogs;
        this.audit = audit;
    }

    @GetMapping("/accounts")
    public List<AccountResponse> allAccounts() {
        return accountService.getAllAccounts().stream().map(AccountResponse::of).toList();
    }

    @GetMapping("/audit-logs")
    public List<AuditLogView> auditLogs(@AuthenticationPrincipal AuthenticatedUser admin,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "50") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 200));
        audit.record(AuditAction.ADMIN_VIEWED_AUDIT_LOG, AuditOutcome.SUCCESS, admin.id(), admin.username(),
                "AUDIT_LOG", null, "page " + safePage);
        return auditLogs.findAllBy(PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id")))
                .getContent().stream().map(AuditLogView::of).toList();
    }
}
