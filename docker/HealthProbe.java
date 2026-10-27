import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Compiled in the build image; no curl package is needed in the runtime image. */
public class HealthProbe {
    public static void main(String[] args) {
        try {
            String port = System.getenv().getOrDefault("PORT", "8080");
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/health"))
                    .timeout(Duration.ofSeconds(6)).GET().build();
            int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            System.exit(status == 200 ? 0 : 1);
        } catch (Exception exception) { System.exit(1); }
    }
}
