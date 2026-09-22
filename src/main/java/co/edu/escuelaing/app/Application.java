package co.edu.escuelaing.app;

import static co.edu.escuelaing.webframework.WebFramework.*;

public class Application {

    public static void main(String[] args) throws Exception {
        String environment = System.getenv().getOrDefault("APP_ENV", "development");

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

        get("/env", (req, resp) -> "APP_ENV=" + environment
                + "\nGREETING_PREFIX=" + System.getenv().getOrDefault("GREETING_PREFIX", "Hello"));

        if (environment.equals("development")) {
            get("/shutdown", (req, resp) -> {
                stop();
                return "Server will stop after this response.";
            });
            System.out.println("Shutdown route enabled (development mode).");
        }

        System.out.println("Environment: " + environment);
        start();
    }
}
