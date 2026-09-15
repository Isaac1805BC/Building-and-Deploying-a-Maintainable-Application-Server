package co.edu.escuelaing.webframework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class WebFramework {

    @FunctionalInterface
    public interface WebService {
        String invoke(Request req, Response resp);
    }

    private static final Map<String, WebService> getRoutes = new HashMap<>();
    private static String staticFilesPath = "/webroot";
    private static boolean running = false;

    public static void staticfiles(String path) {
        staticFilesPath = path;
    }

    public static void get(String path, WebService service) {
        getRoutes.put(path, service);
    }

    public static void start() throws IOException {
        String portValue = System.getenv("PORT");
        start(portValue == null || portValue.isBlank() ? 8080 : Integer.parseInt(portValue));
    }

    public static void start(int port) throws IOException {
        running = true;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);

            while (running) {
                try (Socket clientSocket = serverSocket.accept()) {
                    handleRequest(clientSocket);
                } catch (IOException e) {
                    System.err.println("Error handling request: " + e.getMessage());
                }
            }
        }

        System.out.println("Server stopped gracefully.");
    }

    public static void stop() {
        running = false;
    }

    private static void handleRequest(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
        OutputStream out = clientSocket.getOutputStream();

        String requestLine = in.readLine();
        if (requestLine == null || requestLine.isBlank()) {
            sendError(out, 400, "Bad Request");
            return;
        }

        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            sendError(out, 400, "Bad Request");
            return;
        }

        String method = parts[0];
        String rawPath = parts[1];

        String header;
        while ((header = in.readLine()) != null && !header.isBlank()) {
        }

        if (!method.equals("GET")) {
            sendError(out, 405, "Method Not Allowed");
            return;
        }

        int queryIndex = rawPath.indexOf('?');
        String path = queryIndex == -1 ? rawPath : rawPath.substring(0, queryIndex);
        Map<String, String> queryParams = queryIndex == -1
                ? Map.of()
                : parseQueryParams(rawPath.substring(queryIndex + 1));

        if (path.equals("/")) {
            path = "/index.html";
        }

        WebService service = getRoutes.get(path);
        if (service != null) {
            String body = service.invoke(new Request(queryParams), new Response());
            sendResponse(out, 200, "OK", "text/plain", body.getBytes(StandardCharsets.UTF_8));
            return;
        }

        serveStaticFile(out, path);
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        for (String pair : query.split("&")) {
            if (pair.isBlank()) continue;
            int eq = pair.indexOf('=');
            String key = eq == -1 ? pair : pair.substring(0, eq);
            String value = eq == -1 ? "" : pair.substring(eq + 1);
            params.put(URLDecoder.decode(key, StandardCharsets.UTF_8), URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return params;
    }

    private static void serveStaticFile(OutputStream out, String path) throws IOException {
        String resourcePath = staticFilesPath + path;

        try (var resourceStream = WebFramework.class.getResourceAsStream(resourcePath)) {
            if (resourceStream == null) {
                sendError(out, 404, "Not Found");
                return;
            }
            sendResponse(out, 200, "OK", guessContentType(path), resourceStream.readAllBytes());
        }
    }

    private static String guessContentType(String path) {
        String contentType = URLConnection.guessContentTypeFromName(path);
        if (contentType != null) {
            return contentType;
        }
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        return "application/octet-stream";
    }

    private static void sendError(OutputStream out, int statusCode, String statusText) throws IOException {
        sendResponse(out, statusCode, statusText, "text/plain",
                (statusCode + " " + statusText).getBytes(StandardCharsets.UTF_8));
    }

    private static void sendResponse(OutputStream out, int statusCode, String statusText,
                                      String contentType, byte[] content) throws IOException {
        out.write(("HTTP/1.1 " + statusCode + " " + statusText + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + content.length + "\r\n"
                + "Connection: close\r\n"
                + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(content);
        out.flush();
    }
}
