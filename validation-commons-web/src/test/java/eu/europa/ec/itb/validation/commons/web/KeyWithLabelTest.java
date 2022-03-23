package eu.europa.ec.itb.validation.commons.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyWithLabelTest {

    @Test
    void testCreationAndAccess() {
        var obj = new KeyWithLabel("key1", "label1");
        assertEquals("key1", obj.getKey());
        assertEquals("label1", obj.getLabel());
    }

}
