package eu.europa.ec.itb.validation.commons.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ErrorResponseTypeEnumTest {

    @Test
    void testInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> ErrorResponseTypeEnum.fromValue("BAD"));
    }

}
