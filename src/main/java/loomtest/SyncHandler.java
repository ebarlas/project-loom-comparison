package loomtest;

import org.microhttp.Handler;
import org.microhttp.Header;
import org.microhttp.Request;
import org.microhttp.Response;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class SyncHandler implements Handler {

    final ExecutorService executorService;
    final HttpClient httpClient;
    final String backend;

    SyncHandler(ExecutorService executorService, HttpClient httpClient, String backend) {
        this.executorService = executorService;
        this.httpClient = httpClient;
        this.backend = backend;
    }

    @Override
    public void handle(Request request, Consumer<Response> callback) {
        executorService.execute(() -> callback.accept(doHandle(request)));
    }

    Response doHandle(Request request) {
        try {
            String token = request.header("Authorization");
            if (token == null) {
                throw new RuntimeException("token header required");
            }
            Authentication authentication = sendRequestFor("/authenticate?token=" + token, Authentication.class);
            Authorization authorization = sendRequestFor("/authorize?id=" + authentication.userId(), Authorization.class);
            if (!authorization.authorities().contains("get-meetings")) {
                throw new RuntimeException("not authorized to get meetings");
            }
            Meetings meetings = sendRequestFor("/meetings?id=" + authentication.userId(), Meetings.class);
            List<Header> headers = List.of(new Header("Content-Type", "application/json"));
            return new Response(200, "OK", headers, Json.toJson(meetings));
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(500, "Internal Server Error", List.of(), new byte[0]);
        }
    }

    <T> T sendRequestFor(String endpoint, Class<T> type) throws IOException, InterruptedException {
        URI uri = URI.create("http://%s%s".formatted(backend, endpoint));
        HttpRequest request = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(10))
                .uri(uri)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("error occurred contacting %s".formatted(endpoint));
        }
        return Json.fromJson(response.body(), type);
    }

}
