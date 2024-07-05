package eu.europa.ec.itb.validation.commons.error;

import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class ValidationExceptionTest {

    @Test
    void testDefaultMessage() {
        var ex = new ValidatorException(new IllegalStateException());
        assertEquals("An unexpected error was raised during validation.", ex.getMessage());
        assertFalse(ex.isLocalised());
    }

    @Test
    void testMessages() {
        var ex = new ValidatorException("key.1");
        assertFalse(ex.isLocalised());

        var ex2 = new ValidatorException("key.1", "val1", "val2");
        assertFalse(ex2.isLocalised());
        assertNotNull(ex2.getMessageParams());
        assertEquals(2, ex2.getMessageParams().length);
        assertEquals("val1", ex2.getMessageParams()[0]);
        assertEquals("val2", ex2.getMessageParams()[1]);
        assertEquals("[key.1]", ex2.getMessageForDisplay(new LocalisationHelper(Locale.ENGLISH)));
        assertEquals("[key.1]", ex2.getMessageForLog());

        var ex3 = new ValidatorException("msg1", true);
        assertTrue(ex3.isLocalised());
        assertEquals("msg1", ex3.getMessageForDisplay(new LocalisationHelper(Locale.ENGLISH)));
        assertEquals("msg1", ex3.getMessageForLog());
    }

}
