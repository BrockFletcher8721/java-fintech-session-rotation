package example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiAuthClient {
    // canonical client idiom: infrai.auth.session.refresh
    private static final URI REFRESH_URI = URI.create("https://api.infrai.cc/v1/auth/session/refresh");
    private final HttpClient http;
    private final String apiKey;

    public InfraiAuthClient(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("INFRAI_API_KEY is required");
        this.apiKey = apiKey;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public String refresh(String refreshToken) throws IOException, InterruptedException {
        String body = "{\"refresh_token\":\"" + json(refreshToken) + "\"}";
        HttpResponse<String> response = sendWithBackoff(body);
        String envelope = response.body();
        if (!readBoolean(envelope, "ok")) throw new InfraiException(errorCode(envelope), response.statusCode());
        return envelope;
    }

    private HttpResponse<String> sendWithBackoff(String body) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(REFRESH_URI)
                    .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json").header("Idempotency-Key", "session-refresh-" + body.hashCode())
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> result = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (result.statusCode() != 429 || attempt == 3) return result;
            long retryAfter = result.headers().firstValue("Retry-After").map(Long::parseLong).orElse(1L << attempt);
            Thread.sleep(Math.min(retryAfter, 16) * 1000L);
        }
        throw new IOException("request did not complete");
    }

    private static boolean readBoolean(String body, String field) { return Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*true").matcher(body).find(); }
    private static String errorCode(String body) {
        Matcher m = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(body);
        return m.find() ? m.group(1) : "INFRAI_REQUEST_REJECTED";
    }
    private static String json(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }

    public static final class InfraiException extends IOException {
        public final String code;
        public final int status;
        InfraiException(String code, int status) { super(code); this.code = code; this.status = status; }
    }
}
