package eu.europa.ec.itb.validation.commons.jar;

import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.test.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommandLineValidatorTest extends BaseTest {

    ApplicationContext context;

    @Override
    @BeforeEach
    protected void setup() throws IOException {
        super.setup();
        context = mock(ApplicationContext.class);
    }

    @Override
    @AfterEach
    protected void teardown() {
        super.teardown();
        System.clearProperty("LOG_PATH");
        System.clearProperty("validator.tmpFolder");
        System.clearProperty("validator.resourceRoot");
        reset(context);
    }

    private CommandLineValidator createObject() {
        return new CommandLineValidator() {
            @Override
            protected File createTemporaryFolder(String tempFolderName) throws IOException {
                return Files.createDirectory(Path.of(tmpFolder.toString(), tempFolderName)).toFile();
            }

            @Override
            protected ApplicationContext createContext(Class<?> mainClass, String[] args) {
                return context;
            }
        };
    }

    @Test
    void testStart() throws IOException {
        var workFolder = Path.of(tmpFolder.toString(), "name1", "work");
        var resourceFolder = Path.of(tmpFolder.toString(), "name1", "resources");
        var logsFolder = Path.of(tmpFolder.toString(), "name1", "logs");
        var appConfig = mock(ApplicationConfig.class);
        doReturn(workFolder.toString()).when(appConfig).getTmpFolder();
        doReturn(resourceFolder.toString()).when(appConfig).getResourceRoot();
        var runner = mock(ValidationRunner.class);
        doReturn(appConfig).when(context).getBean(ApplicationConfig.class);
        doReturn(runner).when(context).getBean(ValidationRunner.class);
        var obj = createObject();
        var testArgs = new String[] {"a", "b"};
        obj.start(CommandLineValidatorTest.class, testArgs, "name1");
        assertTrue(Files.exists(Path.of(tmpFolder.toString(), "name1")));
        assertTrue(Files.exists(logsFolder));
        assertTrue(Files.exists(resourceFolder));
        assertTrue(Files.exists(workFolder));
        assertEquals(logsFolder, Path.of(System.getProperty("LOG_PATH")));
        assertEquals(workFolder, Path.of(System.getProperty("validator.tmpFolder")));
        assertEquals(resourceFolder, Path.of(System.getProperty("validator.resourceRoot")));
    }

    @Test
    void testDisableLoggers() throws IllegalStateException {
        resetLogging();
        CommandLineValidator.disableLoggersIfNeeded(false, false);
        assertTrue(CommandLineValidator.isConsoleOutputOn());
        assertTrue(CommandLineValidator.isFileOutputOn());

        resetLogging();
        CommandLineValidator.disableLoggersIfNeeded(true, false);
        assertFalse(CommandLineValidator.isConsoleOutputOn());
        assertTrue(CommandLineValidator.isFileOutputOn());

        resetLogging();
        CommandLineValidator.disableLoggersIfNeeded(false, true);
        assertTrue(CommandLineValidator.isConsoleOutputOn());
        assertFalse(CommandLineValidator.isFileOutputOn());

        resetLogging();
        CommandLineValidator.disableLoggersIfNeeded(true, true);
        assertFalse(CommandLineValidator.isConsoleOutputOn());
        assertFalse(CommandLineValidator.isFileOutputOn());
    }

    private void resetLogging() {
        CommandLineValidator.setConsoleOutputOn(true);
        CommandLineValidator.setFileOutputOn(true);
    }

    @Test
    void testInArray() {
        assertTrue(CommandLineValidator.inArray(List.of("a", "b").toArray(new String[] {}), "a"));
        assertTrue(CommandLineValidator.inArray(List.of("a", "b").toArray(new String[] {}), "A"));
        assertFalse(CommandLineValidator.inArray(List.of("a", "b").toArray(new String[] {}), "c"));
    }
}
