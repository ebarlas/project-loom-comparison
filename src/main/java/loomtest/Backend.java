package loomtest;

import org.microhttp.DebugLogger;
import org.microhttp.EventLoop;
import org.microhttp.Handler;
import org.microhttp.Header;
import org.microhttp.Logger;
import org.microhttp.Options;
import org.microhttp.Request;
import org.microhttp.Response;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Backend {

    public static void main(String[] args) throws IOException {
        Args a = Args.parse(args);
        System.out.println(a);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Handler handler = (req, callback) -> scheduler.schedule(() -> callback.accept(handle(req)), a.delay, TimeUnit.MILLISECONDS);
        Options options = new Options()
                .withHost(a.host)
                .withPort(a.port)
                .withAcceptLength(a.acceptLength)
                .withReuseAddr(true)
                .withReusePort(true);
        Logger logger = a.debug ? new DebugLogger() : new NoOpLogger();
        EventLoop eventLoop = new EventLoop(options, logger, handler);
        eventLoop.start();
    }

    static Response handle(Request req) {
        if (req.uri().startsWith("/authenticate")) {
            return new Response(
                    200,
                    "OK",
                    List.of(new Header("Content-Type", "application/json")),
                    "{\"userId\":\"abc123\"}".getBytes());
        } else if (req.uri().startsWith("/authorize")) {
            return new Response(
                    200,
                    "OK",
                    List.of(new Header("Content-Type", "application/json")),
                    "{\"authorities\":[\"get-meetings\"]}".getBytes());
        } else if (req.uri().startsWith("/meetings")) {
            return new Response(
                    200,
                    "OK",
                    List.of(new Header("Content-Type", "application/json")),
                    "{\"meetings\":[{\"time\":\"now\",\"subject\":\"tech-talk\"}]}".getBytes());
        } else {
            return new Response(404, "Not Found", List.of(), new byte[0]);
        }
    }

    record Args(String host, int port, int delay, int acceptLength, boolean debug) {
        static Args parse(String[] args) {
            return new Args(
                    args.length >= 1 ? args[0] : "localhost",
                    args.length >= 2 ? Integer.parseInt(args[1]) : 8080,
                    args.length >= 3 ? Integer.parseInt(args[2]) : 100,
                    args.length >= 4 ? Integer.parseInt(args[3]) : 0,
                    args.length >= 5 ? Boolean.parseBoolean(args[4]) : true);
        }
    }

}
