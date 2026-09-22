package co.edu.escuelaing.webframework;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Maps an HTTP path to the lambda handler registered for it. */
public class Router {

    private final Map<String, WebService> getRoutes = new ConcurrentHashMap<>();

    public void get(String path, WebService service) {
        getRoutes.put(path, service);
    }

    public Optional<WebService> resolveGet(String path) {
        return Optional.ofNullable(getRoutes.get(path));
    }
}
