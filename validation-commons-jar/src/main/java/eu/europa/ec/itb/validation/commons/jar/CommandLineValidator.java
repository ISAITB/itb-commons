package eu.europa.ec.itb.validation.commons.jar;

import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Objects;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Class that is responsible for setting up a standalone validator. This class prepares the validator's configuration
 * before launching the Spring context.
 */
public class CommandLineValidator {

    /** Static flag to signal whether console output is on. */
    private static boolean consoleOutputOn = true;
    /** Static flag to signal whether file output is on. */
    private static boolean fileOutputOn = true;

    /**
     * @return Whether console output is on.
     */
    public static boolean isConsoleOutputOn() {
        return consoleOutputOn;
    }

    /**
     * @return Whether file output is on.
     */
    public static boolean isFileOutputOn() {
        return fileOutputOn;
    }

    /**
     * @param consoleOutputOn The value for the consoleOutputOn flag.
     */
    public static void setConsoleOutputOn(boolean consoleOutputOn) {
        CommandLineValidator.consoleOutputOn = consoleOutputOn;
    }

    /**
     * @param fileOutputOn The value for the fileOutputOn flag.
     */
    public static void setFileOutputOn(boolean fileOutputOn) {
        CommandLineValidator.fileOutputOn = fileOutputOn;
    }

    /**
     * Run the initialisation.
     *
     * @param mainClass The class to use for launching the Spring context.
     * @param commandLineArguments The command line arguments.
     * @param tempFolderName The folder name to use when creating the overall temp folder for this validation run.
     * @throws IOException If an IO error occurs.
     */
    public void start(Class<?> mainClass, String[] commandLineArguments, String tempFolderName) throws IOException {
        tempFolderName = Objects.requireNonNullElse(tempFolderName, "temp");
        boolean noOutput = inArray(commandLineArguments, BaseValidationRunner.FLAG_NO_OUTPUT);
        boolean noLogs = inArray(commandLineArguments, BaseValidationRunner.FLAG_NO_LOG);
        disableLoggersIfNeeded(noOutput, noLogs);
        if (!noOutput) {
            System.out.print("Starting validator ...");
        }
        File tempFolder = createTemporaryFolder(tempFolderName);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.deleteQuietly(tempFolder)));
        // Setup folders - start
        File resourceFolder = new File(tempFolder, "resources");
        File logFolder = new File(tempFolder, "logs");
        File workFolder = new File(tempFolder, "work");
        if (!resourceFolder.mkdirs() || !logFolder.mkdirs() || !workFolder.mkdirs()) {
            throw new IllegalStateException("Unable to create work directories under ["+tempFolder.getAbsolutePath()+"]");
        }
        // Set the resource root so that it can be used. This is done before app startup to avoid PostConstruct issues.
        String resourceRoot = resourceFolder.getAbsolutePath();
        if (!resourceRoot.endsWith(File.separator)) {
            resourceRoot += File.separator;
        }
        System.setProperty("LOG_PATH", logFolder.getAbsolutePath());
        System.setProperty("validator.tmpFolder", workFolder.getAbsolutePath());
        System.setProperty("validator.resourceRoot", resourceRoot);
        // Setup folders - end
        prepareConfigForStandalone(resourceFolder);
        // Start the application.
        ApplicationContext ctx = createContext(mainClass, commandLineArguments);
        // Post process config.
        // Temporarily disable System.out to avoid printing unwanted messages by Spring (message on commons-logging.jar).
        var defaultSystemOut = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                // No output.
            }
        }));
        ApplicationConfig config = ctx.getBean(ApplicationConfig.class);
        // Restore System.out.
        System.setOut(defaultSystemOut);
        if (!noOutput) {
            System.out.println(" Done.");
        }
        try {
            ValidationRunner runner = ctx.getBean(ValidationRunner.class);
            runner.bootstrap(commandLineArguments, new File(config.getTmpFolder(), UUID.randomUUID().toString()));
        } catch(Exception e) {
            // Ignore errors.
        }
    }

    /**
     * Disable log appending for silent modes.
     *
     * @param disableConsole Disable console logging.
     * @param disableFile Disable file logging.
     */
    protected static void disableLoggersIfNeeded(boolean disableConsole, boolean disableFile) {
        if (disableConsole) {
            consoleOutputOn = false;
        }
        if (disableFile) {
            fileOutputOn = false;
        }
    }

    /**
     * Check to see if the provided array contains the given flag (non-case-sensitive check).
     *
     * @param args The arguments to check.
     * @param flag The flag to look for.
     * @return The check result.
     */
    protected static boolean inArray(String[] args, String flag) {
        return args != null && Arrays.stream(args).anyMatch(flag::equalsIgnoreCase);
    }

    /**
     * Create the Spring application context.
     *
     * @param mainClass The class to use for launching the Spring context.
     * @param args The command line arguments.
     * @return The context.
     */
    protected ApplicationContext createContext(Class<?> mainClass, String[] args) {
        return SpringApplication.run(mainClass, args);
    }

    /**
     * Create the temporary folder for the validator run.
     *
     * @param tempFolderName The name of the folder.
     * @return The folder.
     * @throws IOException If the folder could not be created.
     */
    protected File createTemporaryFolder(String tempFolderName) throws IOException {
        return Files.createTempDirectory(tempFolderName).toFile();
    }

    /**
     * Adapt the validator's configuration so that it can be used as a command-line tool.
     *
     * @param tempFolder The temporary folder to use for the validator's work.
     * @throws IOException If an IO error occurs.
     */
    private void prepareConfigForStandalone(File tempFolder) throws IOException {
        // Explode validator resources to temp folder
        File tempJarFile = new File(tempFolder, "validator-resources.jar");
        FileUtils.copyInputStreamToFile(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("validator-resources.jar")), tempJarFile);
        try (JarFile resourcesJar = new JarFile(tempJarFile)) {
        Enumeration<JarEntry> entries = resourcesJar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                File fileFromJar = new File(tempFolder, entry.getName());
                if (fileFromJar.toPath().normalize().startsWith(tempFolder.toPath())) {
                    if (entry.isDirectory()) { // if it's a directory, create it
                        fileFromJar.mkdir();
                        continue;
                    }
                    FileUtils.copyInputStreamToFile(resourcesJar.getInputStream(entry), fileFromJar);
                }
            }
        }
    }

}
