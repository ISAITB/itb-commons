package eu.europa.ec.itb.validation.commons.jar;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ValidationInputTest {

    @Test
    void testValidationInput() {
        var file = new File("/tmp/test.txt");
        var name = "aName.txt";
        var input = new ValidationInput(file, name);
        assertEquals(file, input.getInputFile());
        assertEquals(name, input.getFileName());
    }

}