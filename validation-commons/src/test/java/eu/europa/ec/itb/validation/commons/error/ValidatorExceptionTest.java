package eu.europa.ec.itb.validation.commons.error;

import eu.europa.ec.itb.validation.commons.LocalisationHelper;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorExceptionTest {

    private final LocalisationHelper helper = new LocalisationHelper(Locale.ENGLISH);

    @Test
    void testMessageWithoutReference() {
        var exception = new ValidatorException("validator.label.exception.base64");
        assertNull(exception.getInputReference());
        assertEquals("Error when processing the provided Base64 data.", exception.getMessageForDisplay(helper));
    }

    @Test
    void testMessageWithReference() {
        var exception = new ValidatorException("validator.label.exception.base64").withInputReference("externalRules[0]");
        assertEquals("externalRules[0]", exception.getInputReference());
        assertEquals("Error in input [externalRules[0]]: Error when processing the provided Base64 data.", exception.getMessageForDisplay(helper));
        assertEquals(exception.getMessageForDisplay(helper), exception.getMessageForLog());
    }

    @Test
    void testInnermostReferenceWins() {
        var exception = new ValidatorException("validator.label.exception.base64")
                .withInputReference("contentToValidate[1]")
                .withInputReference("[2]");
        assertEquals("contentToValidate[1]", exception.getInputReference());
    }

    @Test
    void testReferencePrefix() {
        var nested = new ValidatorException("validator.label.exception.base64").withInputReference("externalRules[0]").withInputReferencePrefix("[2]");
        assertEquals("[2].externalRules[0]", nested.getInputReference());
        var noReference = new ValidatorException("validator.label.exception.base64").withInputReferencePrefix("[1]");
        assertEquals("[1]", noReference.getInputReference());
        assertNull(new ValidatorException("validator.label.exception.base64").withInputReferencePrefix(null).getInputReference());
    }

    @Test
    void testInputPrefixIsFormattedInAllLocales() {
        // Apostrophes in parameterised messages must be escaped (MessageFormat), otherwise placeholders are not replaced.
        for (var language: new String[] {"bg", "cs", "da", "de", "el", "en", "es", "et", "fi", "fr", "ga", "hr", "hu", "it", "lt", "lv", "mt", "nl", "pl", "pt", "ro", "sk", "sl", "sv"}) {
            var message = new LocalisationHelper(Locale.forLanguageTag(language)).localise(ValidatorException.MESSAGE_FOR_INPUT, "REF", "DETAIL");
            assertTrue(message.contains("[REF]") && message.contains("DETAIL"), "Unexpected message for " + language + ": " + message);
            assertFalse(message.contains("{"), "Unreplaced placeholder for " + language + ": " + message);
        }
        assertEquals("Erreur dans l'entrée [REF] : DETAIL", new LocalisationHelper(Locale.FRENCH).localise(ValidatorException.MESSAGE_FOR_INPUT, "REF", "DETAIL"));
    }

    @Test
    void testNullReferenceIgnored() {
        var exception = new ValidatorException("validator.label.exception.base64").withInputReference(null);
        assertNull(exception.getInputReference());
    }

    @Test
    void testLocalisedMessageNotWrappedAgain() {
        var exception = new ValidatorException("Already localised", true).withInputReference("contentToValidate");
        assertEquals("Already localised", exception.getMessageForDisplay(helper));
    }

    @Test
    void testForInputAttachesReferenceToValidatorException() {
        var thrown = assertThrows(ValidatorException.class, () ->
                ValidatorException.forInput("externalSchemas[1]", ValidatorException.MESSAGE_DEFAULT, () -> {
                    throw new ValidatorException("validator.label.exception.base64");
                }));
        assertEquals("externalSchemas[1]", thrown.getInputReference());
    }

    @Test
    void testForInputWrapsOtherExceptions() {
        var cause = new IllegalArgumentException("Bad content");
        var thrown = assertThrows(ValidatorException.class, () ->
                ValidatorException.forInput("contentToValidate[0]", "validator.label.exception.uriReturnedError", () -> {
                    throw cause;
                }));
        assertEquals("contentToValidate[0]", thrown.getInputReference());
        assertSame(cause, thrown.getCause());
        assertArrayEquals(new Object[] {"Bad content"}, thrown.getMessageParams());
    }

    @Test
    void testForInputReturnsResult() {
        assertEquals("OK", ValidatorException.forInput("contentToValidate", ValidatorException.MESSAGE_DEFAULT, () -> "OK"));
    }
}
