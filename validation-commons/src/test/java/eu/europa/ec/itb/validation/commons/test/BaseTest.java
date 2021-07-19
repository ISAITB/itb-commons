package eu.europa.ec.itb.validation.commons.test;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class BaseTest extends TestSupport {

    protected Path tmpFolder;

    @BeforeEach
    protected void setup() throws IOException {
        tmpFolder = Files.createTempDirectory("itb");
    }

    @AfterEach
    protected void teardown() {
        FileUtils.deleteQuietly(tmpFolder.toFile());
    }

}
