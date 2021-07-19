package eu.europa.ec.itb.validation.commons.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ValidationExceptionTest {

    @Test
    void testDefaultMessage() {
        var result = new ValidatorException(new IllegalStateException());
        assertEquals(ValidatorException.MESSAGE_DEFAULT, result.getMessage());
    }

}
