package ledgerline.common;

import java.util.Map;

/** JSON shape of every error response. */
public record ApiError(String error, String message, Map<String, String> fieldErrors) {
}
