package example;

import example.session.SessionRotationPolicy;

public final class RotationExample {
    private record PaymentEvent(String paymentId, String sessionId, int amountCents) {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("provide a refresh token");
        PaymentEvent event = new PaymentEvent("payment-lesson-1", "session-lesson-1", 1250);
        SessionRotationPolicy policy = new SessionRotationPolicy(args[0]);
        String apiKey = System.getenv("INFRAI_API_KEY");
        String envelope = new InfraiAuthClient(apiKey).refresh(args[0]);
        SessionRotationPolicy.Decision decision = policy.rotate(args[0], "replacement-from-envelope");
        System.out.println(event + " -> " + decision + " -> " + envelope);
        System.out.println("AUDIT session=" + event.sessionId() + " generation=" + decision.generation());
    }
}
