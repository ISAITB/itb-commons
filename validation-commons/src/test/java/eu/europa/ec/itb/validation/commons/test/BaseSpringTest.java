package eu.europa.ec.itb.validation.commons.test;

import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public abstract class BaseSpringTest extends BaseTest {

    @Autowired
    protected ApplicationConfig appConfig;

    @Override
    @BeforeEach
    protected void setup() throws IOException {
        super.setup();
        when(appConfig.getTmpFolder()).thenReturn(tmpFolder.toString());
        var resourceRoot = Path.of(tmpFolder.toString(), "resourceRoot");
        Files.createDirectory(resourceRoot);
        when(appConfig.getResourceRoot()).thenReturn(resourceRoot.toString());
    }

    @Override
    @AfterEach
    protected void teardown() {
        super.teardown();
        reset(appConfig);
    }

}
