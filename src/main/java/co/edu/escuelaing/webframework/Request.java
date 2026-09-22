package co.edu.escuelaing.webframework;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Parsed view of an HTTP request: method, path and query-string values. */
public class Request {

    private final String method;
    private final String path;
    private final Map<String, String> queryParams;

    public Request(String method, String path, Map<String, String> queryParams) {
        this.method = method;
        this.path = path;
        this.queryParams = queryParams;
    }

    /**
     * Parses a request line such as {@code GET /hello?name=Pedro HTTP/1.1}.
     *
     * @throws IllegalArgumentException if the line is malformed or the query string is badly encoded
     */
    public static Request parse(String requestLine) {
        if (requestLine == null || requestLine.isBlank()) {
            throw new IllegalArgumentException("Empty request line");
        }
        String[] parts = requestLine.trim().split("\s+");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Incomplete request line");
        }
        String target = parts[1];
        int queryIndex = target.indexOf('?');
        String path = queryIndex == -1 ? target : target.substring(0, queryIndex);
        Map<String, String> params = queryIndex == -1
                ? Map.of()
                : parseQuery(target.substring(queryIndex + 1));
        return new Request(parts[0], path, params);
    }

    static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        for (String pair : query.split("&")) {
            if (pair.isBlank()) continue;
            int eq = pair.indexOf('=');
            String key = eq == -1 ? pair : pair.substring(0, eq);
            String value = eq == -1 ? "" : pair.substring(eq + 1);
            params.put(URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return params;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    /** Returns the query value for {@code name}, or {@code null} when absent. */
    public String getValue(String name) {
        return queryParams.get(name);
    }
}
