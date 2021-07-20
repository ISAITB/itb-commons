package eu.europa.ec.itb.validation.commons;

import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.ObjectFactory;
import com.gitb.tr.TAR;
import eu.europa.ec.itb.validation.commons.artifact.RemoteValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfigCache;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.w3c.dom.Document;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Base class to be extended providing support for common file-system based operations shared by any kind of validator.
 *
 * @param <T> The specific subclass of ApplicationConfig used by the validator in question.
 */
public abstract class BaseFileManager <T extends ApplicationConfig> {

    private static final Logger logger = LoggerFactory.getLogger(BaseFileManager.class);
    private static final JAXBContext REPORT_CONTEXT;
    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    static {
        try {
            REPORT_CONTEXT = JAXBContext.newInstance(TAR.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to create JAXB context for TAR class", e);
        }
    }

    @Autowired
    protected T config = null;
    @Autowired
    protected DomainConfigCache domainConfigCache = null;
    @Autowired
    protected URLReader urlReader = null;
    @Autowired(required = false)
    protected ArtifactPreprocessor preprocessor = null;

    ConcurrentHashMap<String, ReentrantReadWriteLock> externalDomainFileCacheLocks = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, List<FileInfo>> preconfiguredLocalArtifactMap = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, List<FileInfo>> preconfiguredRemoteArtifactMap = new ConcurrentHashMap<>();

    /**
     * Get the file extension to be used for input files.
     *
     * @param contentType The content type (as a mime type) to be considered (if applicable).
     * @return The extension (without the dot).
     */
    public abstract String getFileExtension(String contentType);

    /**
     * Detect and return the content type (as a mime type) for the provided file.
     *
     * @param file The file to process.
     * @param declaredContentType The content type as declared in the validator's inputs.
     * @return The content type (null by default).
     */
    protected String getContentTypeForFile(File file, String declaredContentType) {
        // By default don't try to retrieve the file's type.
        return null;
    }

    /**
     * Check to see if the provided file is accepted as a validation artifact file.
     *
     * @param file The file to check.
     * @param artifactType The artifact type this file corresponds to.
     * @return True if it acceptable (returned also by default if not overridden).
     */
    protected boolean isAcceptedArtifactFile(File file, String artifactType) {
        // By default accept all files.
        return true;
    }

    /**
     * Get the temporary folder used to store files relevant to the validator's usage as a web application.
     *
     * @return The folder to use, a sub-folder beneath the overall temp folder.
     */
    public File getWebTmpFolder() {
        return new File(getTempFolder(), "web");
    }

    /**
     * Get the root temporary folder within which all transient files will be created. Anythins placed within this
     * folder is subject for perioddic deletion if not explicitly deleted.
     *
     * @return The folder.
     */
    public File getTempFolder() {
        return new File(config.getTmpFolder());
    }

    /**
     * Get the folder under which all remotely loaded validation artifacts will be cached. This caching takes place to avoid
     * repeated remote requests for every validation.
     *
     * @return The folder to use, a sub-folder beneath the overall temp folder.
     */
    public File getRemoteFileCacheFolder() {
        return new File(getTempFolder(), "remote_config");
    }

    /**
     * Create a file from the provided BASE64 content.
     *
     * @param targetFolder The folder within which to store the file.
     * @param content The BASE64 content to parse.
     * @param contentType The declared content type of the content (used to determine the file's extension).
     * @return The stored file.
     */
    public File getFileFromBase64(File targetFolder, String content, String contentType) {
        if (targetFolder == null) {
            targetFolder = getWebTmpFolder();
        }
        File tempFile;
        try {
            tempFile = createFile(targetFolder, getFileExtension(contentType)).toFile();
            // Construct the string from its BASE64 encoded bytes.
            byte[] decodedBytes = Base64.getDecoder().decode(content);
            FileUtils.writeByteArrayToFile(tempFile, decodedBytes);
        } catch (IOException e) {
            throw new ValidatorException("Error when processing the provided Base64 data.", e);
        }
        return tempFile;
    }

    /**
     * Create a file from the provided BASE64 content.
     *
     * @param targetFolder The folder within which to store the file.
     * @param content The BASE64 content to parse.
     * @return The stored file.
     */
    public File getFileFromBase64(File targetFolder, String content) {
        return getFileFromBase64(targetFolder, content, null);
    }

    /**
     * Create a file from the provided string which is expected to either be a URL or a BASE64 encoded string. If the
     * string is a URL then a remote call will be made to fetch its contents, otherwise a BASE64 decoding will take place
     * to retrieve the file's bytes.
     *
     * @param targetFolder The folder within which to create the file.
     * @param urlOrBase64 The string to parse as a URL or BASE64 content.
     * @return The stored file.
     * @throws IOException If the string cannot be parsed.
     */
    public File getFileFromURLOrBase64(File targetFolder, String urlOrBase64) throws IOException {
        return getFileFromURLOrBase64(targetFolder, urlOrBase64, null, null);
    }

    /**
     * Create a file from the provided string which is expected to either be a URL or a BASE64 encoded string. If the
     * string is a URL then a remote call will be made to fetch its contents, otherwise a BASE64 decoding will take place
     * to retrieve the file's bytes.
     *
     * @param targetFolder The folder within which to create the file.
     * @param urlOrBase64 The string to parse as a URL or BASE64 content.
     * @param contentType The content type to consider for the content to determine the stored file's extension.
     * @return The stored file.
     * @throws IOException If the string cannot be parsed.
     */
    public File getFileFromURLOrBase64(File targetFolder, String urlOrBase64, String contentType) throws IOException {
        return getFileFromURLOrBase64(targetFolder, urlOrBase64, contentType, null);
    }

    /**
     * Create a file from the provided string which is expected to either be a URL or a BASE64 encoded string. If the
     * string is a URL then a remote call will be made to fetch its contents, otherwise a BASE64 decoding will take place
     * to retrieve the file's bytes.
     *
     * @param targetFolder The folder within which to store the file.
     * @param urlOrBase64 The string to parse as a URL or BASE64 content.
     * @param contentType The content type to consider for the content to determine the stored file's extension.
     * @param artifactType The type of validation artifact this file corresponds to.
     * @return The stored file.
     * @throws IOException If the string cannot be parsed.
     */
    public File getFileFromURLOrBase64(File targetFolder, String urlOrBase64, String contentType, String artifactType) throws IOException {
        if (targetFolder == null) {
            targetFolder = getWebTmpFolder();
        }
        File outputFile;
        try {
            outputFile = getFileFromURL(targetFolder, urlOrBase64, getFileExtension(contentType), null, null, null, artifactType);
        } catch (MalformedURLException e) {
            // Exception means that the text is not a valid URL.
            try {
                outputFile = getFileFromBase64(targetFolder, urlOrBase64, contentType);
            } catch (Exception e2) {
                // This likely means that the is not a valid BASE64 string. Try to get the value as a plain string.
                outputFile = getFileFromString(targetFolder, urlOrBase64, contentType);
            }
        }
        return outputFile;
    }

    /**
     * Store and return the file loaded (and cached) from the provided URL.
     *
     * @param targetFolder The folder to store the file in.
     * @param url The URL to load.
     * @return The stored file.
     * @throws IOException If the file could not be retrieved or stored.
     */
    public File getFileFromURL(File targetFolder, String url) throws IOException {
        return getFileFromURL(targetFolder, url, null, null, null, null, null);
    }

    /**
     * Store and return the file loaded (and cached) from the provided URL.
     *
     * @param targetFolder The folder to store the file in.
     * @param url The URL to load.
     * @param fileName The name of the file to use.
     * @return The stored file.
     * @throws IOException If the file could not be retrieved or stored.
     */
    public File getFileFromURL(File targetFolder, String url, String fileName) throws IOException {
        return getFileFromURL(targetFolder, url, null, fileName, null, null, null);
    }

    /**
     * Store and return the file loaded (and cached) from the provided URL.
     *
     * @param targetFolder The folder to store the file in.
     * @param url The URL to load.
     * @param extension The file extension for the created file.
     * @param fileName The name of the file to use.
     * @return The stored file.
     * @throws IOException If the file could not be retrieved or stored.
     */
    public File getFileFromURL(File targetFolder, String url, String extension, String fileName) throws IOException {
        return getFileFromURL(targetFolder, url, extension, fileName, null, null, null);
    }

    /**
     * Store and return the file loaded (and cached) from the provided URL.
     *
     * @param targetFolder The folder to store the file in.
     * @param url The URL to load.
     * @param extension The file extension for the created file.
     * @param fileName The name of the file to use.
     * @param artifactType The type of validation artifact.
     * @return The stored file.
     * @throws IOException If the file could not be retrieved or stored.
     */
    public File getFileFromURL(File targetFolder, String url, String extension, String fileName, String artifactType) throws IOException {
        return getFileFromURL(targetFolder, url, extension, fileName, null, null, artifactType);
    }

    /**
     * Store and return the file loaded (and cached) from the provided URL.
     *
     * @param targetFolder The folder to store the file in.
     * @param url The URL to load.
     * @param extension The file extension for the created file.
     * @param fileName The name of the file to use.
     * @param preprocessorFile An optional file for a preprocessing resource to be used to determine the final loaded file.
     * @param preprocessorOutputExtension The file extension for the file produced via preprocessing (if applicable).
     * @param artifactType The type of validation artifact.
     * @return The stored file.
     * @throws IOException If the file could not be retrieved or stored.
     */
    public File getFileFromURL(File targetFolder, String url, String extension, String fileName, File preprocessorFile, String preprocessorOutputExtension, String artifactType) throws IOException {
        URL urlObj = new URL(url);
        if (fileName == null) {
            fileName = FilenameUtils.getName(urlObj.getFile());
        }
        if (extension == null) {
            extension = FilenameUtils.getExtension(urlObj.getFile());
        }
        Path targetFilePath = createFile(targetFolder, extension, fileName);
        try (InputStream in = getInputStreamFromURL(url)){
            Files.copy(in, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
        File targetFile = targetFilePath.toFile();
        if (preprocessorFile != null && preprocessor != null) {
            File processedFile = preprocessor.preprocessFile(targetFile, preprocessorFile, preprocessorOutputExtension);
            FileUtils.deleteQuietly(targetFile);
            targetFile = processedFile;
        }
        return targetFile;
    }

    /**
     * Store and return the file created from the provided string content.
     *
     * @param targetFolder The folder within which to create the file.
     * @param content The file's contents.
     * @return The stored file.
     * @throws IOException If a processing error occurs.
     */
    public File getFileFromString(File targetFolder, String content) throws IOException {
        return getFileFromString(targetFolder, content, null, null);
    }

    /**
     * Store and return the file created from the provided string content.
     *
     * @param targetFolder The folder within which to create the file.
     * @param content The file's contents.
     * @param contentType The content type (as a mime type) for the file (to determine its file extension).
     * @return The stored file.
     * @throws IOException If a processing error occurs.
     */
    public File getFileFromString(File targetFolder, String content, String contentType) throws IOException {
        return getFileFromString(targetFolder, content, contentType, null);
    }

    /**
     * Store and return the file created from the provided string content.
     *
     * @param targetFolder The folder within which to create the file.
     * @param content The file's contents.
     * @param contentType The content type (as a mime type) for the file (to determine its file extension).
     * @param fileName The name of the resulting file.
     * @return The stored file.
     * @throws IOException If a processing error occurs.
     */
    public File getFileFromString(File targetFolder, String content, String contentType, String fileName) throws IOException {
        return getFileFromString(targetFolder, content, contentType, fileName, getFileExtension(contentType));
    }

    /**
     * Store and return the file created from the provided string content.
     *
     * @param targetFolder The folder within which to create the file.
     * @param content The file's contents.
     * @param contentType The content type (as a mime type) for the file (to determine its file extension). This is currently ignored.
     * @param fileName The name of the resulting file.
     * @param extension The extension of the created file.
     * @return The stored file.
     * @throws IOException If a processing error occurs.
     */
    public File getFileFromString(File targetFolder, String content, String contentType, String fileName, String extension) throws IOException {
        if (targetFolder == null) {
            targetFolder = getWebTmpFolder();
        }
        Path filePath = createFile(targetFolder, extension, fileName);
        try (InputStream in = new ByteArrayInputStream(content.getBytes())) {
            Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
        return filePath.toFile();
    }

    /**
     * Store and return a file from the provided input stream.
     *
     * @param targetFolder The folder within which to create the file.
     * @param stream The stream to read for the file's contents.
     * @param contentType The content type for the file, used to determine its file extension.
     * @param fileName The name of the resulting file.
     * @return The stored file.
     * @throws IOException If a processing error occurs.
     */
    public File getFileFromInputStream(File targetFolder, InputStream stream, String contentType, String fileName) throws IOException {
        Path tmpPath = createFile(targetFolder, getFileExtension(contentType), fileName);
        Files.copy(stream, tmpPath, StandardCopyOption.REPLACE_EXISTING);
        return tmpPath.toFile();
    }

    /**
     * Open an input stream to read the contents of the provided URL.
     *
     * @param url The URL to load.
     * @return The input stream.
     */
    public InputStream getInputStreamFromURL(String url) {
        // Read the resource from the provided URI.
        return urlReader.stream(URI.create(url));
    }

    /**
     * Create the path for a temporary folder beneath the web temp folder.
     *
     * @return The folder path.
     */
	public File createTemporaryFolderPath() {
		return createTemporaryFolderPath(getWebTmpFolder());
	}

    /**
     * Create the path for a temporary folder beneath the provided parent folder.
     *
     * @param parentFolder The folder within which to create the temporary folder.
     * @return The folder path.
     */
	public File createTemporaryFolderPath(File parentFolder) {
		UUID folderUUID = UUID.randomUUID();
		Path tmpFolder = Paths.get(parentFolder.getAbsolutePath(), folderUUID.toString());
		return tmpFolder.toFile();
	}

    /**
     * Create the path to a file within the provided parent folder.
     *
     * @param parentFolder The folder within which to create the file.
     * @return The file path.
     */
    public Path createFile(File parentFolder) {
        return createFile(parentFolder, null);
    }

    /**
     * Create the path to a file within the provided parent folder with the given extension.
     *
     * @param parentFolder The folder within which to create the file.
     * @param extension The file extension to use.
     * @return The file path.
     */
    public Path createFile(File parentFolder, String extension) {
        return createFile(parentFolder, extension, null);
    }

    /**
     * Create the path to a file within the provided parent folder with the given name and extension.
     *
     * @param parentFolder The folder within which to create the file.
     * @param extension The file extension to use.
     * @param fileName The name of the file to create.
     * @return The file path.
     */
    public Path createFile(File parentFolder, String extension, String fileName) {
        if (StringUtils.isBlank(fileName)) {
            fileName = UUID.randomUUID().toString();
        }
        if (extension == null) {
            extension = "";
        } else if (!extension.isBlank() && !extension.startsWith(".")) {
            extension = "." + extension;
        }
        if (fileName.endsWith(extension)) {
            extension = "";
        }
        Path tmpPath = Paths.get(parentFolder.getAbsolutePath(), fileName + extension);
        tmpPath.toFile().getParentFile().mkdirs();
        return tmpPath;
    }

    /**
     * Run preprocessing on the provided file (considered as a validation artifact) to determine the final file to use in the
     * validator. Preprocessing only takes place if a preprocessor is defined by the validator and the relevant domain,
     * validation type and artifact type define a preprocessing artifact.
     *
     * @param domainConfig The domain configuration to check for preprocessing configuration.
     * @param validationType The validation type linked to the artifact.
     * @param artifactType The artifact's type.
     * @param fileToProcess The artifact's file to process.
     * @param isExternal True if the file is external (user-provided).
     * @return The file file to use. If there is no preprocessor or preprocessing artifact for the specific artifact type
     * this will be the same file provided as the method's original input.
     */
    public File preprocessFileIfNeeded(DomainConfig domainConfig, String validationType, String artifactType, File fileToProcess, boolean isExternal) {
        File outputFile = null;
        if (preprocessor != null) {
            String preProcessorPath;
            String preProcessorOutputExtension;
            if (isExternal) {
                preProcessorPath = domainConfig.getArtifactInfo().get(validationType).get(artifactType).getExternalArtifactPreProcessorPath();
                preProcessorOutputExtension = domainConfig.getArtifactInfo().get(validationType).get(artifactType).getExternalArtifactPreProcessorOutputExtension();
            } else {
                preProcessorPath = domainConfig.getArtifactInfo().get(validationType).get(artifactType).getPreProcessorPath();
                preProcessorOutputExtension = domainConfig.getArtifactInfo().get(validationType).get(artifactType).getPreProcessorOutputExtension();
            }
            if (preProcessorPath != null) {
                outputFile = preprocessFile(domainConfig, fileToProcess, preProcessorPath, preProcessorOutputExtension);
            }
        }
        if (outputFile == null) {
            outputFile = fileToProcess;
        }
        return outputFile;
    }

    /**
     * Run preprocessing on the provided file (considered as a validation artifact) to determine the final file to use in the
     * validator. Preprocessing only takes place if a preprocessor is defined by the validator and the relevant domain,
     * validation type and artifact type define a preprocessing artifact.
     *
     * Note that this method is internal and assumes that preprocessing should indeed proceed.
     *
     * @param domainConfig The domain configuration to check for preprocessing configuration.
     * @param fileToProcess The artifact's file to process.
     * @param preProcessorPath The path to the preprocessing file to use.
     * @param preProcessorOutputExtension The file extension for the resulting final file.
     * @return The file file to use. If there is no preprocessor or preprocessing artifact for the specific artifact type
     * this will be the same file provided as the method's original input.
     */
    private File preprocessFile(DomainConfig domainConfig, File fileToProcess, String preProcessorPath, String preProcessorOutputExtension) {
        File preprocessorFile = Paths.get(config.getResourceRoot(), domainConfig.getDomain(), preProcessorPath).toFile();
        return preprocessor.preprocessFile(fileToProcess, preprocessorFile, preProcessorOutputExtension);
    }

    /**
     * Get the list of preconfigured validation artifacts (local or remote) for the provided domain and validation type. This
     * method assumes that there is only one artifact type for the relevant configuration.
     *
     * @param domainConfig The domain configuration.
     * @param validationType The validation type.
     * @return The list of preconfigured files.
     */
    public List<FileInfo> getPreconfiguredValidationArtifacts(DomainConfig domainConfig, String validationType) {
        return getPreconfiguredValidationArtifacts(domainConfig, validationType, null);
    }

    /**
     * Get the list of preconfigured validation artifacts (local or remote) for the provided domain, validation type and
     * artifact type.
     *
     * @param domainConfig The domain configuration.
     * @param validationType The validation type.
     * @param artifactType The artifact type (can be provided as null if there is only one default one).
     * @return The list of preconfigured files.
     */
    public List<FileInfo> getPreconfiguredValidationArtifacts(DomainConfig domainConfig, String validationType, String artifactType) {
        if (artifactType == null) {
            artifactType = TypedValidationArtifactInfo.DEFAULT_TYPE;
        }
        String key = domainConfig.getDomainName() + "|" + validationType + "|" + artifactType;
        List<FileInfo> allFiles = new ArrayList<>();
        // Local artifacts.
        if (!preconfiguredLocalArtifactMap.containsKey(key)) {
            List<FileInfo> files = new ArrayList<>();
            // Local files.
            boolean preprocess = preprocessor != null && domainConfig.getArtifactInfo().get(validationType).get(artifactType).getPreProcessorPath() != null;
            for (File localFile : getLocalValidationArtifactFiles(domainConfig, validationType, artifactType)) {
                List<FileInfo> loadedArtifacts = getLocalValidationArtifacts(localFile, artifactType);
                if (!preprocess) {
                    files.addAll(loadedArtifacts);
                } else {
                    for (FileInfo fileInfo : loadedArtifacts) {
                        File processedFile = preprocessFile(domainConfig, fileInfo.getFile(), domainConfig.getArtifactInfo().get(validationType).get(artifactType).getPreProcessorPath(), domainConfig.getArtifactInfo().get(validationType).get(artifactType).getPreProcessorOutputExtension());
                        files.add(new FileInfo(processedFile));
                    }
                }
            }
            preconfiguredLocalArtifactMap.put(key, files);
        }
        allFiles.addAll(preconfiguredLocalArtifactMap.get(key));
        // Remote files - these are already pre-processed as part of the bootstrap.
        if (!preconfiguredRemoteArtifactMap.containsKey(key)) {
            logger.warn("Remote validation artifacts were not found to be cached. This is not normal as caching is done via the periodic reload.");
            preconfiguredRemoteArtifactMap.put(key, getRemoteValidationArtifacts(domainConfig, validationType, artifactType));
        }
        allFiles.addAll(preconfiguredRemoteArtifactMap.get(key));
        return allFiles;
    }

    /**
     * Get the validation artifacts for the provided domain, validation type and artifact type and are part of the validator's
     * locally preconfigured artifacts (i.e. provided on the file system).
     *
     * @param domainConfig The domain configuration.
     * @param validationType The validation type.
     * @param artifactType The artifact type.
     * @return The list of validation artifacts.
     */
    private List<File> getLocalValidationArtifactFiles(DomainConfig domainConfig, String validationType, String artifactType) {
        List<File> localFileReferences = new ArrayList<>();
        String localFolderConfigValue = domainConfig.getArtifactInfo().get(validationType).get(StringUtils.defaultString(artifactType, TypedValidationArtifactInfo.DEFAULT_TYPE)).getLocalPath();
        if (StringUtils.isNotEmpty(localFolderConfigValue)) {
            String[] localFiles = StringUtils.split(localFolderConfigValue, ',');
            for (String localFile: localFiles) {
                try {
                    if (config.isRestrictResourcesToDomain()) {
                        if ((new File(localFile.trim())).isAbsolute() || !domainConfigCache.isInDomainFolder(domainConfig.getDomain(), localFile)) {
                            throw new ValidatorException(String.format("Resources are restricted to domain. Their paths should be relative to domain folder. Unable to load file %s", localFile));
                        } else {
                            localFileReferences.add(Paths.get(config.getResourceRoot(), domainConfig.getDomain(), localFile.trim()).toFile().getCanonicalFile());
                        }
                    } else {
                        if (new File(localFile.trim()).isAbsolute()) {
                            localFileReferences.add(Paths.get(localFile.trim()).toFile());
                        } else {
                            localFileReferences.add(Paths.get(config.getResourceRoot(), domainConfig.getDomain(), localFile.trim()).toFile().getCanonicalFile());
                        }
                    }
                } catch (IOException e) {
                    throw new ValidatorException("Unable to read local validation artifacts", e);
                }
            }
        }
        return localFileReferences;
    }

    /**
     * Get the validation artifacts corresponding to the provided file or folder. If this is a file then the artifact is the
     * file itself, otherwise if it is a folder is is all the files contained within the folder (top-level only).
     *
     * @param fileOrFolder The file or folder to check for artifacts.
     * @param artifactType In case of a folder this artifact type is used to determine which files are indeed validation
     *                     artifacts. This is done by means of the isAcceptedArtifactFile method.
     * @return The list of validation artifacts.
     */
    public List<FileInfo> getLocalValidationArtifacts(File fileOrFolder, String artifactType) {
        List<FileInfo> fileInfo = new ArrayList<>();
        if (fileOrFolder != null && fileOrFolder.exists()) {
            if (fileOrFolder.isFile()) {
                // We are pointing to a single file.
                fileInfo.add(new FileInfo(fileOrFolder, getContentTypeForFile(fileOrFolder, null)));
            } else {
                // All files are loaded are processed.
                File[] files = fileOrFolder.listFiles();
                if (files != null) {
                    for (File currentFile: files) {
                        if (currentFile.isFile()) {
                            if (isAcceptedArtifactFile(currentFile, artifactType)) {
                                fileInfo.add(new FileInfo(currentFile, getContentTypeForFile(currentFile, null)));
                            }
                        }
                    }
                }
            }
        } else {
        	throw new ValidatorException("Unable to find validation file " + ((fileOrFolder != null)? fileOrFolder.getPath(): ""));
        }
        return fileInfo;
    }


    /**
     * Get the list of validation artifacts for the provided domain, validation type and artifact type that are loaded from
     * remote resources (i.e. provided as URLs).
     *
     * Remote files are loaded and cached as part of the validator's startup. As such this method does not actually make remote
     * calls but rather looks up the already cached local copies of the artifacts' files.
     *
     * @param domainConfig The domain configuration.
     * @param validationType The validation type.
     * @param artifactType The atrifact type.
     * @return The list of validation artifacts.
     */
    public List<FileInfo> getRemoteValidationArtifacts(DomainConfig domainConfig, String validationType, String artifactType) {
        File remoteConfigFolder = new File(new File(new File(getRemoteFileCacheFolder(), domainConfig.getDomainName()), validationType), artifactType);
        if (remoteConfigFolder.exists()) {
            return getLocalValidationArtifacts(remoteConfigFolder, artifactType);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Process the provided artifact contents and return them as external (user-provided) artifacts to consider for the validation.
     * These artifacts are first stored temporarily on the filesystem and, if configured to do so, are pre-processed to
     * provide the final artifacts to use.
     *
     * @param domainConfig The domain configuration.
     * @param validationType The validation type.
     * @param artifactType The artifact type.
     * @param parentFolder The folder within which to create a temporary folder to store all external artifacts.
     * @param externalArtifactContents The contents of the artifacts to consider, store and preprocess (if applicable).
     * @return The list of artifacts to use.
     */
    public List<FileInfo> getExternalValidationArtifacts(DomainConfig domainConfig, String validationType, String artifactType, File parentFolder, List<FileContent> externalArtifactContents) {
        List<FileInfo> externalArtifactFiles = new ArrayList<>();
        if (externalArtifactContents != null && !externalArtifactContents.isEmpty()) {
            File externalArtifactFolder = new File(parentFolder, UUID.randomUUID().toString());
            for (FileContent externalArtifact: externalArtifactContents) {
                File file = storeFileContent(externalArtifactFolder, externalArtifact, artifactType);
                externalArtifactFiles.add(new FileInfo(file, getContentTypeForFile(file, externalArtifact.getContentType())));
            }
        }
        List<FileInfo> filesToReturn;
        // Apply pre-processing (if needed).
        if (preprocessor != null && domainConfig.getArtifactInfo().get(validationType).get(artifactType).getExternalArtifactPreProcessorPath() != null) {
            filesToReturn = new ArrayList<>();
            for (FileInfo fileInfo: externalArtifactFiles) {
                filesToReturn.add(new FileInfo(preprocessFile(domainConfig, fileInfo.getFile(), domainConfig.getArtifactInfo().get(validationType).get(artifactType).getExternalArtifactPreProcessorPath(), domainConfig.getArtifactInfo().get(validationType).get(artifactType).getExternalArtifactPreProcessorOutputExtension())));
            }
        } else {
            filesToReturn = externalArtifactFiles;
        }
        return filesToReturn;
    }

    /**
     * Initialisation method triggered on bootstrap that (a) ensures the temporary folder is clear, and (b) does the initial
     * loading and caching of all remote artifacts.
     */
    @PostConstruct
    public void init() {
        FileUtils.deleteQuietly(getTempFolder());
        for (Object config: domainConfigCache.getAllDomainConfigurations()) {
            getExternalDomainFileCacheLock(((DomainConfig)config).getDomainName());
        }
        FileUtils.deleteQuietly(getRemoteFileCacheFolder());
        resetRemoteFileCache();
    }

    /**
     * Get or create the external file lock used to ensure cache resets don't take place while validation is ongoing.
     *
     * @param domainName The domain name.
     * @return The lock.
     */
    ReentrantReadWriteLock getExternalDomainFileCacheLock(String domainName) {
        return externalDomainFileCacheLocks.computeIfAbsent(domainName, (key) -> new ReentrantReadWriteLock());
    }

    /**
     * Recurrent method to clean up temporary web folders that have exceed the inactivity threshold.
     */
    @Scheduled(fixedDelayString = "${validator.cleanupWebRate}")
    public void removeWebFiles() {
        logger.debug("Remove web file cache");
        long currentMillis = System.currentTimeMillis();
        File webFolder = getWebTmpFolder();
        if (webFolder.exists()) {
            File[] files = webFolder.listFiles();
            if (files != null) {
                for (File file: files) {
                    if (currentMillis - file.lastModified() > config.getCleanupWebRate()) {
                        FileUtils.deleteQuietly(file);
                    }
                }
            }
        }
        File reportFolder = getReportFolder();
        if (reportFolder.exists()) {
            File[] files = reportFolder.listFiles();
            if (files != null) {
                for (File file: files) {
                    if (currentMillis - file.lastModified() > config.getCleanupWebRate()) {
                        FileUtils.deleteQuietly(file);
                    }
                }
            }
        }
    }

    /**
     * Recurring method to clean up and refresh the cached remote artefacts.
     */
    @Scheduled(fixedDelayString = "${validator.cleanupRate}")
    public void resetRemoteFileCache() {
        logger.debug("Resetting remote validation artifact cache");
        for (Object domainConfigObj: domainConfigCache.getAllDomainConfigurations()) {
            DomainConfig domainConfig = (DomainConfig)domainConfigObj;
            try {
                // Get write lock for domain.
                logger.debug("Waiting for lock to reset cache for ["+domainConfig.getDomainName()+"]");
                getExternalDomainFileCacheLock(domainConfig.getDomainName()).writeLock().lock();
                logger.debug("Locked cache for ["+domainConfig.getDomainName()+"]");
                try {
                    for (String validationType: domainConfig.getType()) {
                        // Empty cache folder.
                        File remoteConfigFolder = new File(new File(getRemoteFileCacheFolder(), domainConfig.getDomainName()), validationType);
                        FileUtils.deleteQuietly(remoteConfigFolder);
                        TypedValidationArtifactInfo typedArtifactInfo = domainConfig.getArtifactInfo().get(validationType);
                        for (String artifactType: typedArtifactInfo.getTypes()) {
                            artifactType = StringUtils.defaultString(artifactType, TypedValidationArtifactInfo.DEFAULT_TYPE);
                            File remoteFolderForType = new File(remoteConfigFolder, artifactType);
                            downloadRemoteFiles(domainConfig.getDomain(), typedArtifactInfo.get(artifactType).getRemoteArtifacts(), remoteFolderForType, artifactType);
                            // Update cache map.
                            String key = domainConfig.getDomainName() + "|" + validationType + "|" + StringUtils.defaultString(artifactType, TypedValidationArtifactInfo.DEFAULT_TYPE);
                            preconfiguredRemoteArtifactMap.put(key, getRemoteValidationArtifacts(domainConfig, validationType, artifactType));
                        }
                    }
                } catch (Exception e) {
                    // Never allow configuration errors in one domain to prevent the others from being available.
                    logger.error("Error while processing configuration for domain ["+domainConfig.getDomainName()+"]", e);
                }
            } finally {
                // Unlock domain.
                getExternalDomainFileCacheLock(domainConfig.getDomainName()).writeLock().unlock();
                logger.debug("Reset remote validation artifact cache for ["+domainConfig.getDomainName()+"]");
            }
        }
    }

    /**
     * Download and store the provided set of remote validation artifacts.
     *
     * @param domain The domain to consider.
     * @param remoteFiles The list of information per remote file to be loaded.
     * @param remoteConfigPath The folder in which to store the resulting files.
     * @param artifactType The artifact type in question.
     * @throws IOException If a processing error occurs.
     */
    private void downloadRemoteFiles(String domain, List<RemoteValidationArtifactInfo> remoteFiles, File remoteConfigPath, String artifactType) throws IOException {
        if (remoteFiles != null) {
            for (RemoteValidationArtifactInfo artifactInfo: remoteFiles) {
                File preprocessorFile = null;
                if (artifactInfo.getPreProcessorPath() != null) {
                    preprocessorFile = Paths.get(config.getResourceRoot(), domain, artifactInfo.getPreProcessorPath()).toFile();
                }
                getFileFromURL(remoteConfigPath, artifactInfo.getUrl(), null, null, preprocessorFile, artifactInfo.getPreProcessorOutputExtension(), artifactType);
            }
        }
    }

    /**
     * Get the temporary folder used to store validation reports.
     *
     * @return The folder, as a sub-folder of the validator's overall temp folder.
     */
    public File getReportFolder() {
        return new File(getTempFolder(), "reports");
    }

    /**
     * Save the provided TAR report in the validator's temporary file system.
     *
     * @param report The report to serialise and persist.
     * @param uuid The identifier to use when constructing the file's name.
     * @param domainConfig The domain's configuration.
     * @param <R> The specific type of domain configuration subclass.
     */
    public <R extends DomainConfig> void saveReport(TAR report, String uuid, R domainConfig) {
        File outputFile = new File(getReportFolder(), "TAR-"+uuid+".xml");
        saveReport(report, outputFile, domainConfig);
    }

    /**
     * Save the provided TAR report in the validator's temporary file system.
     *
     * @param report The report to serialise and persist.
     * @param outputFile The file as which to save the serialised report.
     * @param domainConfig The domain's configuration.
     * @param <R> The specific type of domain configuration subclass.
     */
    public <R extends DomainConfig> void saveReport(TAR report, File outputFile, R domainConfig) {
        try {
            Marshaller m = REPORT_CONTEXT.createMarshaller();
            outputFile.getParentFile().mkdirs();

            // Apply XML report limit for report items
            if (report.getReports() != null && report.getReports().getInfoOrWarningOrError().size() > domainConfig.getMaximumReportsForXmlOutput()) {
                report.getReports().getInfoOrWarningOrError().subList(domainConfig.getMaximumReportsForXmlOutput().intValue(), report.getReports().getInfoOrWarningOrError().size()).clear();
            }

            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            Document document = docBuilderFactory.newDocumentBuilder().newDocument();
            m.marshal(OBJECT_FACTORY.createTestStepReport(report), document);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "{http://www.gitb.com/core/v1/}value");
            try (OutputStream fos = new FileOutputStream(outputFile)) {
                transformer.transform(new DOMSource(document), new StreamResult(fos));
                fos.flush();
            } catch(IOException e) {
                logger.warn("Unable to save XML report", e);
            }

        } catch (Exception e) {
            logger.warn("Unable to marshal XML report", e);
        }
    }

    /**
     * Signal the start of a validation. The purpose of this is to lock the remote artifact cache to ensure it does
     * not change during validation.
     *
     * @param domainName The name of the domain in question.
     */
    public void signalValidationStart(String domainName) {
        logger.debug("Signalling validation start for ["+domainName+"]");
        getExternalDomainFileCacheLock(domainName).readLock().lock();
        logger.debug("Signalled validation start for ["+domainName+"]");
    }

    /**
     * Signal the end of a validation. This releases the locks preventing remote file cache refreshes.
     *
     * @param domainName The name of the domain in question.
     */
    public void signalValidationEnd(String domainName) {
        logger.debug("Signalling validation end for ["+domainName+"]");
        getExternalDomainFileCacheLock(domainName).readLock().unlock();
        logger.debug("Signalled validation end for ["+domainName+"]");
    }

    /**
     * Store a file based on the provided content.
     *
     * @param targetFolder The folder within which to create the file.
     * @param content The content string (can be a URL, BASE64 string or the content directly).
     * @param embeddingMethod The method determining how the content string is to be handled.
     * @param contentType The type of the content (as a mime type).
     * @param artifactType The artifact type.
     * @return The stored file.
     */
    public File storeFileContent(File targetFolder, String content, ValueEmbeddingEnumeration embeddingMethod, String contentType, String artifactType) {
        File file;
        if (content != null) {
            if (embeddingMethod != null) {
                switch (embeddingMethod) {
                    case STRING:
                        // Use string as-is.
                        try {
                            file = getFileFromString(targetFolder, content, contentType);
                        } catch (IOException e) {
                            throw new ValidatorException("Unable to process provided string value", e);
                        }
                        break;
                    case URI:
                        // Read the string from the provided URI.
                        try {
                            file = getFileFromURL(targetFolder, content, getFileExtension(contentType), null, null, null, artifactType);
                        } catch (IOException e) {
                            throw new ValidatorException("Unable to process provided URI resource", e);
                        }
                        break;
                    default: // BASE_64
                        // Construct the string from its BASE64 encoded bytes.
                        file = getFileFromBase64(targetFolder, content, contentType);
                        break;
                }
            } else {
                try {
                    file = getFileFromURLOrBase64(targetFolder, content, null, artifactType);
                } catch (IOException e) {
                    throw new ValidatorException("Unable to save content (URL or BASE64) to file", e);
                }
            }
        } else {
            throw new ValidatorException("Unable to store empty content");
        }
        return file;
    }

    /**
     * Store a file based on the provided content.
     *
     * @param targetFolder The folder within which to create the file.
     * @param content The content string (can be a URL, BASE64 string or the content directly).
     * @param embeddingMethod The method determining how the content string is to be handled.
     * @param artifactType The artifact type.
     * @return The stored file.
     */
    public File storeFileContent(File targetFolder, String content, ValueEmbeddingEnumeration embeddingMethod, String artifactType) {
        return storeFileContent(targetFolder, content, embeddingMethod, null, artifactType);
    }

    /**
     * Store a file based on the provided content.
     *
     * @param targetFolder The folder within which to create the file.
     * @param content The content's information (can be a URL, BASE64 string or the content directly).
     * @param artifactType The artifact type.
     * @return The stored file.
     */
    public File storeFileContent(File targetFolder, FileContent content, String artifactType) {
        return storeFileContent(targetFolder, content.getContent(), content.getEmbeddingMethod(), content.getContentType(), artifactType);
    }

}
