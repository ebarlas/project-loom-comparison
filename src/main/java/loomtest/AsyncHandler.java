package loomtest;

import org.microhttp.Handler;
import org.microhttp.Header;
import org.microhttp.Request;
import org.microhttp.Response;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AsyncHandler implements Handler {

    final HttpClient httpClient;
    final String backend;

    AsyncHandler(HttpClient httpClient, String backend) {
        this.httpClient = httpClient;
        this.backend = backend;
    }

    public void handle(Request request, Consumer<Response> callback) {
        String token = request.header("Authorization");
        CompletableFuture<String> tokenFtr = token != null
                ? CompletableFuture.completedFuture(token)
                : CompletableFuture.failedFuture(new RuntimeException("token header required"));
        CompletableFuture<Authentication> authentication = tokenFtr
                .thenCompose(t -> sendRequestFor("/authenticate?token=" + t, Authentication.class));
        CompletableFuture<Authorization> authorization = authentication
                .thenCompose(a -> sendRequestFor("/authorize?id=" + a.userId(), Authorization.class))
                .thenApply(a -> {
                    if (!a.authorities().contains("get-meetings")) {
                        throw new RuntimeException("not authorized to get meetings");
                    }
                    return a;
                });
        CompletableFuture<Meetings> meetings = authorization
                .thenCompose(a -> sendRequestFor("/meetings?id=" + authentication.join().userId(), Meetings.class));
        List<Header> headers = List.of(new Header("Content-Type", "application/json"));
        CompletableFuture<Response> response = meetings
                .thenApply(meets -> new Response(200, "OK", headers, Json.toJson(meets)));
        response.whenComplete((res, exception) -> {
            if (exception != null) {
                exception.printStackTrace();
                callback.accept(new Response(500, "Internal Server Error", List.of(), new byte[0]));
            } else {
                callback.accept(res);
            }
        });
    }

    <T> CompletableFuture<T> sendRequestFor(String endpoint, Class<T> type) {
        URI uri = URI.create("http://%s%s".formatted(backend, endpoint));
        HttpRequest request = HttpRequest.newBuilder()
                .timeout(Duration.ofSeconds(15))
                .uri(uri)
                .GET()
                .build();
        CompletableFuture<HttpResponse<String>> response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        return response.thenApply(res -> Json.fromJson(res.body(), type));
    }

}
