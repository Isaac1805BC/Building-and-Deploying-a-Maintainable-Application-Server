package co.edu.escuelaing.webframework;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class StaticFileServiceTest {

    private final StaticFileService service = new StaticFileService();

    @Test
    void servesHtmlWithContentType() throws IOException {
        var file = service.find("/index.html").orElseThrow();
        assertTrue(file.contentType().startsWith("text/html"));
    }

    @Test
    void servesBinaryImage() throws IOException {
        var file = service.find("/images/logo.png").orElseThrow();
        assertEquals("image/png", file.contentType());
        assertTrue(file.content().length > 0);
    }

    @Test
    void missingFileIsEmpty() throws IOException {
        assertTrue(service.find("/missing.txt").isEmpty());
    }

    @Test
    void pathTraversalIsRejected() throws IOException {
        assertTrue(service.find("/../../pom.xml").isEmpty());
        assertTrue(service.find("/..").isEmpty());
    }
}
