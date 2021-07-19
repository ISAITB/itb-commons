package eu.europa.ec.itb.validation.commons.web.errors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class NotFoundExceptionTest {

    @Test
    void testDomainInfo() {
        var exception = new NotFoundException("domain1");
        assertEquals("domain1", exception.getRequestedDomain());
    }

    @Test
    void testNoDomainInfo() {
        var exception = new NotFoundException();
        assertNull(exception.getRequestedDomain());
    }

}
