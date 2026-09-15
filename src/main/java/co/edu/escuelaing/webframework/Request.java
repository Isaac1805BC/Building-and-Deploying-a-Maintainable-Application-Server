package co.edu.escuelaing.webframework;

import java.util.Map;

public class Request {

    private final Map<String, String> queryParams;

    public Request(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public String getValue(String name) {
        return queryParams.get(name);
    }
}
