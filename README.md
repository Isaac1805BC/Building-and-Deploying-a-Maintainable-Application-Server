# Building and Deploying a Maintainable Application Server

A small Java web framework (no external dependencies) that serves static files (HTML, CSS, JS, images) and lets developers register HTTP `GET` services with lambdas, plus a demo application built on top of it. The server is **sequential** (one request at a time), configured through **environment variables**, and packaged as a Docker image for cloud deployment.

- **Cloud platform:** _pending, see [Cloud deployment](#cloud-deployment)_
- **Public URL:** _pending_

## Framework API

```java
import static co.edu.escuelaing.webframework.WebFramework.*;

public class Application {
    public static void main(String[] args) throws Exception {
        staticfiles("/webroot");

        get("/hello", (req, resp) -> {
            String name = req.getValue("name");
            if (name == null || name.isBlank()) name = "world";
            return System.getenv().getOrDefault("GREETING_PREFIX", "Hello") + " " + name;
        });
        get("/pi", (req, resp) -> String.valueOf(Math.PI));

        if (System.getenv().getOrDefault("APP_ENV", "development").equals("development")) {
            get("/shutdown", (req, resp) -> { stop(); return "Server will stop after this response."; });
        }
        start(); // reads PORT (default 8080)
    }
}
```

Adding a route never touches the connection loop.

## Architecture

```
Application                (registers routes + configuration)
    │
WebFramework               (facade: staticfiles(), get(), start(), stop())
    │
HttpServer                 (accept loop, request parsing, response writing)
    ├── Request / Response (HTTP data abstractions)
    ├── Router  ───────────► WebService lambdas  (/hello, /pi, /env, /shutdown)
    └── StaticFileService  (fallback: HTML, CSS, JS, images from /webroot)
```

Request flow: parse method, path and query → look up a dynamic route in the `Router` → if found run its lambda → otherwise ask the `StaticFileService` → otherwise `404 Not Found`.

### Components

| Class | Responsibility |
|---|---|
| `WebFramework` | Public facade; reads `PORT` and `STATIC_FILES_PATH`. |
| `HttpServer` | Sequential accept loop, HTTP parsing/response, error mapping (400/404/405/408/500), graceful stop. |
| `Router` | Maps a path to a `WebService` lambda. |
| `WebService` | Functional interface `String invoke(Request, Response)`. |
| `Request` | Method, path and decoded query parameters (`getValue`). Rejects malformed request lines. |
| `Response` | Status, content type and headers a handler may tweak. |
| `StaticFileService` | Serves text and binary files from the classpath (or an external directory), blocks path traversal, picks the Content-Type. |
| `Application` | Demo app: routes and environment-based configuration. |

### Metaphor: an office building

| Building | Framework |
|---|---|
| Entrance and receptionist (attends one visitor at a time) | `HttpServer` |
| Directory in the lobby | `Router` |
| Individual offices, each with one service | Lambda handlers (`/hello`, `/pi`, `/env`) |
| Document archive | `StaticFileService` (HTML, CSS, JS, images) |
| Building configuration (opening hours, address, rules) | Environment variables (`PORT`, `APP_ENV`, `GREETING_PREFIX`) |
| Malformed visitor / unknown office | 400 / 404 responses, the building keeps operating |
| Closing procedure: finish serving the current visitor, then lock up | Graceful shutdown (`/shutdown`, development only) |

To add a service you add an office and a line in the directory. The reception desk does not change. The master key to close the building (`/shutdown`) is only handed out in development.

## Why this architecture is maintainable

- **Separation of concerns / high cohesion:** sockets and HTTP, routing, static files and business logic live in different classes.
- **Low coupling / extensibility:** new endpoints are registered with `get(path, lambda)`; the server loop is never modified.
- **Abstraction:** developers never touch sockets.
- **Externalized configuration:** port, environment and greeting are environment variables, so the same artifact runs locally and in the cloud.
- **Testability:** `Request`, `Router`, `StaticFileService` and `HttpServer` are tested independently (see [Tests](#tests)).
- **Robustness:** failing handlers give 500, bad requests 400, idle clients time out (5 s) and the server keeps running.

## Build and run locally

Requirements: JDK 17+ and Maven 3.9+.

```bash
mvn clean package                       # compiles and runs the tests
java -jar target/webframework-jar-with-dependencies.jar
# open http://localhost:8080
```

With custom configuration:

```bash
PORT=9090 GREETING_PREFIX=Hola APP_ENV=development java -jar target/webframework-jar-with-dependencies.jar
```

PowerShell: `$env:PORT="9090"; java -jar target\webframework-jar-with-dependencies.jar`

### Docker

```bash
docker build -t webframework .
docker run --rm -p 8080:8080 -e APP_ENV=production -e GREETING_PREFIX=Hola webframework
```

## Environment variables

| Variable | Purpose | Default |
|---|---|---|
| `PORT` | HTTP port (the server binds to all interfaces, not just localhost) | `8080` |
| `APP_ENV` | Execution environment. `/shutdown` exists only when it is `development` | `development` |
| `GREETING_PREFIX` | Prefix used by `/hello` | `Hello` |
| `STATIC_FILES_PATH` | Optional external directory for static files | classpath `/webroot` |

No secrets are stored in the repository.

## Endpoints and examples

| URL | Type | Result |
|---|---|---|
| `/` or `/index.html` | static | Demo page |
| `/styles.css`, `/app.js` | static | CSS and JavaScript |
| `/images/logo.png` | static (binary) | Image |
| `/hello?name=Pedro&language=en` | lambda | `Hello Pedro` |
| `/hello` | lambda | `Hello world` |
| `/pi` | lambda | `3.141592653589793` |
| `/env` | lambda | Shows `APP_ENV` and `GREETING_PREFIX` (non-sensitive) |
| `/shutdown` | lambda (development only) | Stops the server gracefully |
| `/unknown` | - | `404 Not Found` |

The page uses `fetch()` in `app.js` to call `/hello` and `/pi` asynchronously.

## Tests

`mvn test` runs 17 JUnit tests:

- `RequestTest`: query extraction, several parameters, URL decoding, missing parameter, malformed request lines.
- `RouterTest`: registered and unknown routes.
- `StaticFileServiceTest`: HTML, binary PNG, missing file, path traversal (`/../../pom.xml`).
- `HttpServerTest` (real sockets): lambda route, static file, 404 with `text/plain`, 400 and server survives, handler exception gives 500 and server survives, 405, `/shutdown` responds and then stops the server.

### Manual tests (local, `PORT=9090 GREETING_PREFIX=Hola`, development)

```
GET /hello?name=Pedro&language=en  -> 200 "Hola Pedro"
GET /hello                         -> 200 "Hola world"
GET /pi                            -> 200 "3.141592653589793"
GET /env                           -> 200 "APP_ENV=development / GREETING_PREFIX=Hola"
GET /index.html                    -> 200 text/html; charset=utf-8
GET /app.js                        -> 200 application/javascript; charset=utf-8
GET /styles.css                    -> 200 text/css; charset=utf-8
GET /images/logo.png               -> 200 image/png (256 bytes)
GET /unknown                       -> 404 Not Found (text/plain) "404 Not Found"
GET /../pom.xml                    -> 404 Not Found
GARBAGE                            -> 400 Bad Request
GET /hello?name=%zz                -> 400 Bad Request
```

### `/shutdown` in development

```
$ curl -i localhost:9090/shutdown
HTTP/1.1 200 OK ...
Server will stop after this response.

# server log
Shutdown route enabled (development mode).
Environment: development
Server listening on port 9090
Server stopped gracefully.

$ curl localhost:9090/pi   -> connection refused (server is down)
```

### `/shutdown` in production

```
$ PORT=9091 APP_ENV=production java -jar ...
$ curl -i localhost:9091/shutdown
HTTP/1.1 404 Not Found
404 Not Found
$ curl localhost:9091/env
APP_ENV=production
GREETING_PREFIX=Hello
```

## Cloud deployment

The repository includes a multi-stage `Dockerfile` (Maven build, then a JRE-only image), so any container platform works. Generic steps (Railway/Render/Fly/AWS App Runner):

1. Create a service from this GitHub repository (it will use the `Dockerfile`).
2. Set the variables `APP_ENV=production` and `GREETING_PREFIX=Hola`. Do **not** set `PORT` manually if the platform injects it.
3. Generate a public domain and open it.
4. Verify: `/`, `/images/logo.png`, `/hello?name=Pedro`, `/pi`, `/env` respond, and `/shutdown` returns `404`.

### Cloud evidence

> **Pending:** deployed URL, screenshot of the page, `/images/logo.png`, `/hello`, `/pi`, `/env` (showing `APP_ENV=production`) and `/shutdown` returning 404 must be added here once the service is published.
