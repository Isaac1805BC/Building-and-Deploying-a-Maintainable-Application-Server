package co.edu.escuelaing.webframework;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class RouterTest {

    @Test
    void resolvesRegisteredRoute() {
        Router router = new Router();
        router.get("/pi", (req, resp) -> "3.14");
        String body = router.resolveGet("/pi").orElseThrow()
                .invoke(new Request("GET", "/pi", Map.of()), new Response());
        assertEquals("3.14", body);
    }

    @Test
    void unknownRouteIsEmpty() {
        assertTrue(new Router().resolveGet("/nope").isEmpty());
    }
}
