package loomtest;

import org.microhttp.DebugLogger;
import org.microhttp.EventLoop;
import org.microhttp.Handler;
import org.microhttp.Logger;
import org.microhttp.Options;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Frontend {

    public static void main(String[] args) throws IOException {
        Args a = Args.parse(args);
        System.out.println(a);
        Options options = new Options()
                .withHost(a.host)
                .withPort(a.port)
                .withAcceptLength(a.acceptLength)
                .withReuseAddr(true)
                .withReusePort(true);
        Logger logger = a.debug ? new DebugLogger() : new NoOpLogger();
        EventLoop eventLoop = new EventLoop(options, logger, handler(a));
        eventLoop.start();
    }

    static Handler handler(Args args) {
        return args.type.equals("async")
                ? new AsyncHandler(httpClient(args.type), args.backend)
                : new SyncHandler(executorService(args), httpClient(args.type), args.backend);
    }

    static ExecutorService executorService(Args args) {
        return args.type.contains("vthread")
                ? Executors.newVirtualThreadPerTaskExecutor()
                : Executors.newFixedThreadPool(args.threads);
    }

    static HttpClient httpClient(String type) {
        if (type.equals("vthread")) {
            return HttpClient.newBuilder()
                    .executor(Executors.newVirtualThreadPerTaskExecutor())
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
        }
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    record Args(String host, int port, String type, int threads, String backend, int acceptLength, boolean debug) {
        static Args parse(String[] args) {
            return new Args(
                    args.length >= 1 ? args[0] : "localhost",
                    args.length >= 2 ? Integer.parseInt(args[1]) : 8081,
                    args.length >= 3 ? args[2] : "vthread",
                    args.length >= 4 ? Integer.parseInt(args[3]) : 10,
                    args.length >= 5 ? args[4] : "localhost:8080",
                    args.length >= 6 ? Integer.parseInt(args[5]) : 0,
                    args.length >= 7 ? Boolean.parseBoolean(args[6]) : true);
        }
    }

}
