package example.session;

import java.util.Objects;

public final class SessionRotationPolicy {
    private String activeRefreshToken;
    private boolean revoked;
    private int generation;

    public SessionRotationPolicy(String initialRefreshToken) {
        this.activeRefreshToken = Objects.requireNonNull(initialRefreshToken);
    }

    public Decision rotate(String presentedToken, String replacementToken) {
        if (revoked) return Decision.rejected("SESSION_REVOKED");
        if (!Objects.equals(activeRefreshToken, presentedToken)) {
            return Decision.rejected("REFRESH_TOKEN_ALREADY_ROTATED");
        }
        activeRefreshToken = Objects.requireNonNull(replacementToken);
        generation++;
        return Decision.accepted(generation);
    }

    public void revoke() { revoked = true; }

    public record Decision(boolean accepted, String reason, int generation) {
        static Decision accepted(int generation) { return new Decision(true, "ROTATED", generation); }
        static Decision rejected(String reason) { return new Decision(false, reason, -1); }
    }
}
