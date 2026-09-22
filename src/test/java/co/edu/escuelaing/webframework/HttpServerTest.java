package co.edu.escuelaing.webframework;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpServerTest {

    private HttpServer server;
    private Thread thread;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        Router router = new Router();
        router.get("/hello", (req, resp) -> "Hello " + req.getValue("name"));
        router.get("/boom", (req, resp) -> {
            throw new IllegalStateException("boom");
        });
        router.get("/shutdown", (req, resp) -> {
            server.stop();
            return "bye";
        });
        server = new HttpServer(router, new StaticFileService());
        thread = new Thread(() -> {
            try {
                server.start(port);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        thread.start();
        for (int i = 0; i < 50 && !server.isRunning(); i++) {
            Thread.sleep(50);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (thread.isAlive()) {
            server.stop();
            request("GET /index.html HTTP/1.1");
            thread.join(2000);
        }
    }

    private String request(String requestLine) throws Exception {
        try (Socket socket = new Socket("localhost", port)) {
            socket.getOutputStream().write((requestLine + "\r\nHost: localhost\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            socket.getInputStream().transferTo(buffer);
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }

    @Test
    void dynamicRouteReturnsLambdaResult() throws Exception {
        String response = request("GET /hello?name=Pedro HTTP/1.1");
        assertTrue(response.startsWith("HTTP/1.1 200 OK"));
        assertTrue(response.endsWith("Hello Pedro"));
    }

    @Test
    void staticFileIsServed() throws Exception {
        String response = request("GET /index.html HTTP/1.1");
        assertTrue(response.startsWith("HTTP/1.1 200 OK"));
        assertTrue(response.contains("text/html"));
    }

    @Test
    void unknownResourceReturns404() throws Exception {
        String response = request("GET /unknown HTTP/1.1");
        assertTrue(response.startsWith("HTTP/1.1 404 Not Found"));
        assertTrue(response.contains("Content-Type: text/plain"));
        assertTrue(response.endsWith("404 Not Found"));
    }

    @Test
    void malformedRequestReturns400AndServerSurvives() throws Exception {
        assertTrue(request("GARBAGE").startsWith("HTTP/1.1 400"));
        assertTrue(request("GET /x?a=%zz HTTP/1.1").startsWith("HTTP/1.1 400"));
        assertTrue(request("GET /hello HTTP/1.1").startsWith("HTTP/1.1 200"));
    }

    @Test
    void failingHandlerReturns500AndServerSurvives() throws Exception {
        assertTrue(request("GET /boom HTTP/1.1").startsWith("HTTP/1.1 500"));
        assertTrue(request("GET /hello HTTP/1.1").startsWith("HTTP/1.1 200"));
    }

    @Test
    void nonGetMethodReturns405() throws Exception {
        assertTrue(request("POST /hello HTTP/1.1").startsWith("HTTP/1.1 405"));
    }

    @Test
    void shutdownRouteRespondsThenStopsServer() throws Exception {
        String response = request("GET /shutdown HTTP/1.1");
        assertTrue(response.startsWith("HTTP/1.1 200 OK"));
        assertTrue(response.endsWith("bye"));
        thread.join(3000);
        assertFalse(thread.isAlive());
    }
}
