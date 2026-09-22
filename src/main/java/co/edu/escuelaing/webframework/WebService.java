package co.edu.escuelaing.webframework;

/** Handler that produces the body of a dynamic HTTP response. */
@FunctionalInterface
public interface WebService {
    String invoke(Request req, Response resp);
}
