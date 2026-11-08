package ledgerline.security;

import ledgerline.user.Role;

/** The identity of the caller, taken from the server-side session (never from request data). */
public record AuthenticatedUser(Long id, String username, Role role) {
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
