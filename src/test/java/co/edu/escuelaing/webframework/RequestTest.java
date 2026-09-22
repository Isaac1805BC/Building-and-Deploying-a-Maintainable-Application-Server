package co.edu.escuelaing.webframework;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RequestTest {

    @Test
    void parsesMethodPathAndQuery() {
        Request r = Request.parse("GET /hello?name=Pedro&language=en HTTP/1.1");
        assertEquals("GET", r.getMethod());
        assertEquals("/hello", r.getPath());
        assertEquals("Pedro", r.getValue("name"));
        assertEquals("en", r.getValue("language"));
    }

    @Test
    void missingParameterReturnsNull() {
        assertNull(Request.parse("GET /hello HTTP/1.1").getValue("name"));
    }

    @Test
    void decodesEncodedValues() {
        assertEquals("Ana Maria", Request.parse("GET /hello?name=Ana%20Maria HTTP/1.1").getValue("name"));
    }

    @Test
    void malformedRequestLinesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Request.parse(""));
        assertThrows(IllegalArgumentException.class, () -> Request.parse(null));
        assertThrows(IllegalArgumentException.class, () -> Request.parse("GET"));
        assertThrows(IllegalArgumentException.class, () -> Request.parse("GET /x?a=%zz HTTP/1.1"));
    }
}
