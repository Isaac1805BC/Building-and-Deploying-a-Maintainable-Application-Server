package co.edu.escuelaing.app;

import static co.edu.escuelaing.webframework.WebFramework.*;

public class Application {

    public static void main(String[] args) throws Exception {
        staticfiles("/webroot");

        get("/hello", (req, resp) -> {
            String name = req.getValue("name");
            if (name == null || name.isBlank()) {
                name = "world";
            }
            String greetingPrefix = System.getenv()
                    .getOrDefault("GREETING_PREFIX", "Hello");
            return greetingPrefix + " " + name;
        });

        get("/pi", (req, resp) -> String.valueOf(Math.PI));

        start();
    }
}
