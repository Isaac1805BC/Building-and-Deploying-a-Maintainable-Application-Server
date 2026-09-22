package co.edu.escuelaing.webframework;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Serves static files (text and binary). Files come from an external directory when one is
 * configured, otherwise from the classpath.
 */
public class StaticFileService {

    /** A static resource ready to be sent. */
    public record StaticFile(byte[] content, String contentType) {
    }

    private String classpathRoot = "/webroot";
    private Path externalRoot;

    /** Uses a classpath location such as {@code /webroot}. */
    public void setClasspathLocation(String location) {
        this.classpathRoot = location.startsWith("/") ? location : "/" + location;
        this.externalRoot = null;
    }

    /** Uses a filesystem directory instead of the classpath. */
    public void setExternalDirectory(String directory) {
        this.externalRoot = Paths.get(directory).toAbsolutePath().normalize();
    }

    public Optional<StaticFile> find(String path) throws IOException {
        if (path.contains("\0") || path.contains("\\")) {
            return Optional.empty();
        }
        for (String segment : path.split("/")) {
            if (segment.equals("..")) {
                return Optional.empty();
            }
        }
        String normalized = path.replaceAll("^/+", "");
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        byte[] content = externalRoot != null ? readExternal(normalized) : readClasspath(normalized);
        if (content == null) {
            return Optional.empty();
        }
        return Optional.of(new StaticFile(content, contentTypeFor(normalized)));
    }

    private byte[] readExternal(String relative) throws IOException {
        Path file = externalRoot.resolve(relative).normalize();
        if (!file.startsWith(externalRoot) || !Files.isRegularFile(file)) {
            return null;
        }
        return Files.readAllBytes(file);
    }

    private byte[] readClasspath(String relative) throws IOException {
        try (InputStream in = StaticFileService.class.getResourceAsStream(classpathRoot + "/" + relative)) {
            return in == null ? null : in.readAllBytes();
        }
    }

    static String contentTypeFor(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=utf-8";
        if (lower.endsWith(".css")) return "text/css; charset=utf-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (lower.endsWith(".json")) return "application/json; charset=utf-8";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        String guessed = URLConnection.guessContentTypeFromName(path);
        return guessed != null ? guessed : "application/octet-stream";
    }
}
