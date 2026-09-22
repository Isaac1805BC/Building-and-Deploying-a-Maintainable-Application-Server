package co.edu.escuelaing.webframework;

import java.io.IOException;

/** Public facade of the framework: staticfiles(), get(), start() and stop(). */
public class WebFramework {

    private static final Router router = new Router();
    private static final StaticFileService staticFiles = new StaticFileService();
    private static final HttpServer server = new HttpServer(router, staticFiles);

    private WebFramework() {
    }

    public static void staticfiles(String path) {
        staticFiles.setClasspathLocation(path);
    }

    public static void get(String path, WebService service) {
        router.get(path, service);
    }

    /** Starts on the port given by the PORT environment variable (default 8080). */
    public static void start() throws IOException {
        start(resolvePort(System.getenv("PORT")));
    }

    public static void start(int port) throws IOException {
        String externalPath = System.getenv("STATIC_FILES_PATH");
        if (externalPath != null && !externalPath.isBlank()) {
            staticFiles.setExternalDirectory(externalPath);
        }
        server.start(port);
    }

    public static void stop() {
        server.stop();
    }

    static int resolvePort(String portValue) {
        if (portValue == null || portValue.isBlank()) {
            return 8080;
        }
        try {
            int port = Integer.parseInt(portValue.trim());
            if (port < 0 || port > 65535) {
                throw new IllegalArgumentException("PORT out of range: " + port);
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("PORT must be a number, got: " + portValue);
        }
    }
}
