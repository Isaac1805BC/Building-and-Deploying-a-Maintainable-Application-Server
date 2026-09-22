package co.edu.escuelaing.webframework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Sequential HTTP server: accepts one connection at a time, parses the request, asks the
 * {@link Router} for a dynamic handler and falls back to the {@link StaticFileService}.
 */
public class HttpServer {

    private static final int CLIENT_TIMEOUT_MS = 5000;

    private final Router router;
    private final StaticFileService staticFiles;
    private volatile boolean running = false;

    public HttpServer(Router router, StaticFileService staticFiles) {
        this.router = router;
        this.staticFiles = staticFiles;
    }

    public void start(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            running = true;
            System.out.println("Server listening on port " + port);
            while (running) {
                try (Socket clientSocket = serverSocket.accept()) {
                    clientSocket.setSoTimeout(CLIENT_TIMEOUT_MS);
                    handleRequest(clientSocket);
                } catch (IOException e) {
                    System.err.println("Error handling request: " + e.getMessage());
                } catch (RuntimeException e) {
                    System.err.println("Unexpected error: " + e);
                }
            }
        }
        System.out.println("Server stopped gracefully.");
    }

    /** Lets the current request finish, then exits the accept loop. */
    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    private void handleRequest(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
        OutputStream out = clientSocket.getOutputStream();

        String requestLine;
        try {
            requestLine = in.readLine();
            String header;
            while ((header = in.readLine()) != null && !header.isBlank()) {
                // headers are not needed by this framework
            }
        } catch (SocketTimeoutException e) {
            sendText(out, 408, "Request Timeout");
            return;
        }

        Request request;
        try {
            request = Request.parse(requestLine);
        } catch (IllegalArgumentException e) {
            sendText(out, 400, "Bad Request");
            return;
        }

        if (!request.getMethod().equals("GET")) {
            sendText(out, 405, "Method Not Allowed");
            return;
        }

        String path = request.getPath().equals("/") ? "/index.html" : request.getPath();

        Optional<WebService> service = router.resolveGet(path);
        if (service.isPresent()) {
            invokeService(out, service.get(), request);
            return;
        }

        Optional<StaticFileService.StaticFile> file = staticFiles.find(path);
        if (file.isPresent()) {
            send(out, 200, "OK", file.get().contentType(), file.get().content());
        } else {
            sendText(out, 404, "Not Found");
        }
    }

    private void invokeService(OutputStream out, WebService service, Request request) throws IOException {
        Response response = new Response();
        String body;
        try {
            body = service.invoke(request, response);
        } catch (RuntimeException e) {
            System.err.println("Handler failed for " + request.getPath() + ": " + e);
            sendText(out, 500, "Internal Server Error");
            return;
        }
        byte[] content = (body == null ? "" : body).getBytes(StandardCharsets.UTF_8);
        send(out, response.getStatusCode(), response.getStatusText(), response.getContentType(), content);
    }

    private void sendText(OutputStream out, int status, String text) throws IOException {
        send(out, status, text, "text/plain; charset=utf-8",
                (status + " " + text).getBytes(StandardCharsets.UTF_8));
    }

    private void send(OutputStream out, int status, String statusText, String contentType, byte[] content)
            throws IOException {
        out.write(("HTTP/1.1 " + status + " " + statusText + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + content.length + "\r\n"
                + "Connection: close\r\n"
                + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(content);
        out.flush();
    }
}
