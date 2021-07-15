package eu.europa.ec.itb.validation.commons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ValidatorChannelTest {

    @Test
    void testDefinedValues() {
        assertEquals("form", ValidatorChannel.FORM.getName());
        assertEquals("email", ValidatorChannel.EMAIL.getName());
        assertEquals("rest_api", ValidatorChannel.REST_API.getName());
        assertEquals("soap_api", ValidatorChannel.SOAP_API.getName());
    }

    @Test
    void testByNameOk() {
        assertEquals(ValidatorChannel.REST_API, ValidatorChannel.byName("rest_api"));
    }

    @Test
    void testByNameNok() {
        assertThrows(IllegalArgumentException.class, () -> ValidatorChannel.byName("rest_api_bad"));
    }

}
