package example.session;

public final class SessionRotationPolicyTest {
    public static void main(String[] args) {
        SessionRotationPolicy policy = new SessionRotationPolicy("first");
        check(policy.rotate("first", "second").accepted(), "first refresh must rotate");
        check(!policy.rotate("first", "third").accepted(), "replaying the old token must be rejected");
        policy.revoke();
        check(!policy.rotate("second", "fourth").accepted(), "revoked sessions must stay closed");
        System.out.println("session rotation decisions passed");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
