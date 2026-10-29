import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * NIMS Attendance backend (plain Java, no libraries needed).
 * Run:  java Server.java        then open http://localhost:8080
 * GET  /api/db  -> returns the saved data (db.json)
 * PUT  /api/db  -> saves the data
 * /             -> serves index.html
 */
public class Server {
    static final Path DB = Paths.get("db.json");
    static final Path PAGE = Paths.get("index.html");
    static final Object LOCK = new Object();

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer s = HttpServer.create(new InetSocketAddress(port), 0);
        s.createContext("/api/db", Server::api);
        s.createContext("/", Server::page);
        s.start();
        System.out.println("NIMS Attendance running at http://localhost:" + port);
    }

    static void send(HttpExchange x, int code, String type, byte[] body) throws IOException {
        x.getResponseHeaders().set("Content-Type", type);
        x.getResponseHeaders().set("Cache-Control", "no-store");
        if (body.length == 0) {
            x.sendResponseHeaders(code, -1);
        } else {
            x.sendResponseHeaders(code, body.length);
            try (OutputStream o = x.getResponseBody()) { o.write(body); }
        }
        x.close();
    }

    static void api(HttpExchange x) throws IOException {
        synchronized (LOCK) {
            String m = x.getRequestMethod();
            if (m.equals("GET")) {
                byte[] b = Files.exists(DB) ? Files.readAllBytes(DB)
                        : "null".getBytes(StandardCharsets.UTF_8);
                send(x, 200, "application/json; charset=utf-8", b);
            } else if (m.equals("PUT")) {
                Files.write(DB, x.getRequestBody().readAllBytes());
                send(x, 200, "application/json", "{\"ok\":true}".getBytes(StandardCharsets.UTF_8));
            } else {
                send(x, 405, "text/plain", new byte[0]);
            }
        }
    }

    static void page(HttpExchange x) throws IOException {
        String p = x.getRequestURI().getPath();
        if (!p.equals("/") && !p.equals("/index.html")) { send(x, 404, "text/plain", new byte[0]); return; }
        send(x, 200, "text/html; charset=utf-8", Files.readAllBytes(PAGE));
    }
}
