import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class HealthCheck {

  private static final Duration TIMEOUT = Duration.ofSeconds(3);

  private HealthCheck() {}

  public static void main(String[] args) throws Exception {
    String port = System.getenv().getOrDefault("SERVER_PORT", "8080");
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:" + port + "/actuator/health/liveness"))
        .header("X-Forwarded-Proto", "https")
        .timeout(TIMEOUT)
        .GET()
        .build();

    try (HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build()) {
      HttpResponse<Void> response =
          client.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() != 200) {
        throw new IllegalStateException("Liveness endpoint returned HTTP " + response.statusCode());
      }
    }
  }
}
