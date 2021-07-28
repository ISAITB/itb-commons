package eu.europa.ec.itb.validation.commons.jar;

import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

    /**
     * Run the initialisation.
     *
     * @param mainClass The class to use for launching the Spring context.
     * @param commandLineArguments The command line arguments.
     * @param tempFolderName The folder name to use when creating the overall temp folder for this validation run.
     * @throws IOException If an IO error occurs.
     */
    public void start(Class<?> mainClass, String[] commandLineArguments, String tempFolderName) throws IOException {
        System.out.print("Starting validator ...");
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
        ApplicationConfig config = ctx.getBean(ApplicationConfig.class);
        System.out.println(" Done.");
        try {
            ValidationRunner runner = ctx.getBean(ValidationRunner.class);
            runner.bootstrap(commandLineArguments, new File(config.getTmpFolder(), UUID.randomUUID().toString()));
        } catch(Exception e) {
            // Ignore errors.
        }
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
        JarFile resourcesJar = new JarFile(tempJarFile);
        Enumeration<JarEntry> entries = resourcesJar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            File f = new File(tempFolder, entry.getName());
            if (entry.isDirectory()) { // if its a directory, create it
                f.mkdir();
                continue;
            }
            FileUtils.copyInputStreamToFile(resourcesJar.getInputStream(entry), f);
        }
    }

}
