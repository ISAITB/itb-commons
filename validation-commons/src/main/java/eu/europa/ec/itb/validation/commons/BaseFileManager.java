/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package eu.europa.ec.itb.validation.commons;

import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.ObjectFactory;
import com.gitb.tr.TAR;
import eu.europa.ec.itb.validation.commons.artifact.RemoteValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfigCache;
import eu.europa.ec.itb.validation.commons.config.ErrorResponseTypeEnum;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static eu.europa.ec.itb.validation.commons.Utils.limitReportItemsIfNeeded;

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
    protected DomainConfigCache<?> domainConfigCache = null;
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
    protected String getContentTypeForFile(FileInfo file, String declaredContentType) {
        // By default, don't try to retrieve the file's type.
        return null;
    }

    /**
     * Detect and return the content type (as a mime type) for the provided file.
     *
     * @param file The file to process.
     * @return The content type (null by default).
     */
    private String getContentTypeForFile(File file) {
        // By default, don't try to retrieve the file's type.
        return getContentTypeForFile(new FileInfo(file), null);
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
        return getFileFromBase64(targetFolder, content, contentType, null);
    }

    /**
     * Create a file from the provided BASE64 content.
     *
     * @param targetFolder The folder within which to store the file.
     * @param content The BASE64 content to parse.
     * @param contentType The declared content type of the content (used to determine the file's extension).
     * @param fileName The file name to use.
     * @return The stored file.
     */
    public File getFileFromBase64(File targetFolder, String content, String contentType, String fileName) {
        return getFileFromBase64(targetFolder, content, contentType, fileName, true);
    }

    /**
     * Create a file from the provided BASE64 content.
     *
     * @param targetFolder The folder within which to store the file.
     * @param content The BASE64 content to parse.
     * @param contentType The declared content type of the content (used to determine the file's extension).
     * @param fileName The file name to use.
     * @param tryAlsoAsMimeEncoded In case of BAse64 parse failure, whether to also try to parse an Mime-encoded (RFC2045).
     * @return The stored file.
     */
    private File getFileFromBase64(File targetFolder, String content, String contentType, String fileName, boolean tryAlsoAsMimeEncoded) {
        if (targetFolder == null) {
            targetFolder = getWebTmpFolder();
        }
        File tempFile;
        try {
            tempFile = createFile(targetFolder, getFileExtension(contentType), fileName).toFile();
            // Construct the string from its BASE64 encoded bytes.
            byte[] decodedBytes = Utils.decodeBase64String(content, tryAlsoAsMimeEncoded);
            FileUtils.writeByteArrayToFile(tempFile, decodedBytes);
        } catch (IOException e) {
            throw new ValidatorException("validator.label.exception.base64", e);
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
     * @param httpVersion The HTTP version to use.
     * @return The stored file.
     * @throws IOException If the string cannot be parsed.
     */
    public FileInfo getFileFromURLOrBase64(File targetFolder, String urlOrBase64, HttpClient.Version httpVersion) throws IOException {
        return getFileFromURLOrBase64(targetFolder, urlOrBase64, null, null, httpVersion);
    }

    /**
     * Create a file from the provided string which is expected to either be a URL or a BASE64 encoded string. If the
     * string is a URL then a remote call will be made to fetch its contents, otherwise a BASE64 decoding will take place
     * to retrieve the file's bytes.
     *
     * @param targetFolder The folder within which to create the file.
     * @param urlOrBase64 The string to parse as a URL or BASE64 content.
     * @param contentType The content type to consider for the content to determine the stored file's extension.
     * @param httpVersion The HTTP version to use.
     * @return The stored file.
     * @throws IOException If the string cannot be parsed.
     */
    public FileInfo getFileFromURLOrBase64(File targetFolder, String urlOrBase64, String contentType, HttpClient.Version httpVersion) throws IOException {
        return getFileFromURLOrBase64(targetFolder, urlOrBase64, contentType, null, httpVersion);
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
     * @param httpVersion The HTTP version to use.
     * @return The stored file.
     * @throws IOException If the string cannot be parsed.
     */
    public FileInfo getFileFromURLOrBase64(File targetFolder, String urlOrBase64, String contentType, String artifactType, HttpClient.Version httpVersion) throws IOException {
        return getFileFromURLOrBase64(targetFolder, urlOrBase64, contentType, artifactType, null, httpVersion);
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
     * @param fileName The file name to use.
     * @param httpVersion The HTTP version to use.
     * @return The stored file.
     * @throws IOException If the string cannot be parsed.
     */
    public FileInfo getFileFromURLOrBase64(File targetFolder, String urlOrBase64, String contentType, String artifactType, String fileName, HttpClient.Version httpVersion) throws IOException {
        if (targetFolder == null) {
            targetFolder = getWebTmpFolder();
        }
        FileInfo outputFile;
        try {
            outputFile = getFileFromURL(targetFolder, urlOrBase64, getFileExtension(contentType), fileName, null, null, artifactType, StringUtils.isEmpty(contentType)?null:List.of(contentType), httpVersion);
        } catch (MalformedURLException e) {
            // Exception means that the text is not a valid URL.
            try {
                outputFile = new FileInfo(getFileFromBase64(targetFolder, urlOrBase64, contentType, fileName, false), contentType);
            } catch (Exception e2) {
                // This likely means that the is not a valid BASE64 string. Try to get the value as a plain string.
                outputFile = new FileInfo(getFileFromString(targetFolder, urlOrBase64, contentType, fileName), contentType);
            }
        }
        return outputFile;
    }

    /**
     * Store and return the file loaded (and cached) from the provided URL.
     *
     * @param targetFolder The folder to store the file in.
     * @param url The URL to load.
     * @param httpVersion The HTTP version to use.
     * @return The stored file.
     * @throws IOException If the file could not be retrieved or stored.
     */
    public File getFileFromURL(File targetFolder, String url, HttpClient.Version httpVersion) throws IOException {
        return getFileFromURL(targetFolder, url, null, null, null, null, null, null, httpVersion).getFile();
    }

    /**
     * Store and return the file loaded (and cached) from the provided URL.
     *
     * @param targetFolder The folder to store the file in.
     * @param url The URL to load.
     * @param fileName The name of the file to use.
     * @param httpVersion The HTTP version to use.
     * @return The stored file.
     * @throws IOException If the file could not be retrieved or stored.
     */
    public File getFileFromURL(File targetFolder, String url, String fileName, HttpClient.Version httpVersion) throws IOException {
        return getFileFromURL(targetFolder, url, null, fileName, null, null, null, null, httpVersion).getFile();
    }

    /**
     * Store and return the file loaded (and cached) from the provided URL.
     *
     * @param targetFolder The folder to store the file in.
     * @param url The URL to load.
     * @param extension The file extension for the created file.
     * @param fileName The name of the file to use.
     * @param httpVersion The HTTP version to use.
     * @return The stored file.
     * @throws IOException If the file could not be retrieved or stored.
     */
    public File getFileFromURL(File targetFolder, String url, String extension, String fileName, HttpClient.Version httpVersion) throws IOException {
        return getFileFromURL(targetFolder, url, extension, fileName, null, null, null, null, httpVersion).getFile();
    }

    /**
     * Store and return the file loaded (and cached) from the provided URL.
     *
     * @param targetFolder The folder to store the file in.
     * @param url The URL to load.
     * @param extension The file extension for the created file.
     * @param fileName The name of the file to use.
     * @param artifactType The type of validation artifact.
     * @param httpVersion The HTTP version to use.
     * @return The stored file.
     * @throws IOException If the file could not be retrieved or stored.
     */
    public File getFileFromURL(File targetFolder, String url, String extension, String fileName, String artifactType, HttpClient.Version httpVersion) throws IOException {
        return getFileFromURL(targetFolder, url, extension, fileName, null, null, artifactType, null, httpVersion).getFile();
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
     * @param acceptedContentTypes A (nullable) list of content types to accept for the request.
     * @param httpVersion The HTTP version to use.
     * @return The stored file and the returned content type from the remote URI.
     * @throws IOException If the file could not be retrieved or stored.
     */
    public FileInfo getFileFromURL(File targetFolder, String url, String extension, String fileName, File preprocessorFile, String preprocessorOutputExtension, String artifactType, List<String> acceptedContentTypes, HttpClient.Version httpVersion) throws IOException {
        URL urlObj = Utils.parseUrl(url);
        if (fileName == null) {
            fileName = FilenameUtils.getName(urlObj.getPath());
        }
        if (extension == null) {
            extension = FilenameUtils.getExtension(urlObj.getPath());
        }
        StreamInfo streamInfo = null;
        Path targetFilePath;
        try {
            streamInfo = getInputStreamFromURL(url, acceptedContentTypes, httpVersion);
            if (StringUtils.isEmpty(extension) && streamInfo.contentType().isPresent()) {
                extension = getFileExtension(streamInfo.contentType().get());
            }
            targetFilePath = createFile(targetFolder, extension, fileName);
            Files.copy(streamInfo.stream(), targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            if (streamInfo != null) IOUtils.close(streamInfo.stream());
        }
        File targetFile = targetFilePath.toFile();
        if (preprocessorFile != null && preprocessor != null) {
            File processedFile = preprocessor.preprocessFile(targetFile, preprocessorFile, preprocessorOutputExtension);
            FileUtils.deleteQuietly(targetFile);
            targetFile = processedFile;
        }
        return new FileInfo(targetFile, streamInfo.contentType().orElse(null));
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
     * @param acceptedContentTypes A (nullable) list of content types to accept for the request.
     * @param httpVersion The HTTP version to use.
     * @return The input stream.
     */
    public StreamInfo getInputStreamFromURL(String url, List<String> acceptedContentTypes, HttpClient.Version httpVersion) {
        // Read the resource from the provided URI.
        return urlReader.stream(URI.create(StringUtils.defaultString(url).trim()), acceptedContentTypes, httpVersion);
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
     * <p>
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
        final String artifactTypeToUse = Objects.requireNonNullElse(artifactType, TypedValidationArtifactInfo.DEFAULT_TYPE);
        String key = domainConfig.getDomainName() + "|" + validationType + "|" + artifactTypeToUse;
        // Local artifacts.
        if (!preconfiguredLocalArtifactMap.containsKey(key)) {
            List<FileInfo> files = new ArrayList<>();
            // Local files.
            boolean preprocess = preprocessor != null && domainConfig.getArtifactInfo().get(validationType).get(artifactTypeToUse).getPreProcessorPath() != null;
            for (File localFile : getLocalValidationArtifactFiles(domainConfig, validationType, artifactTypeToUse)) {
                List<FileInfo> loadedArtifacts = getLocalValidationArtifacts(localFile, artifactTypeToUse);
                if (!preprocess) {
                    files.addAll(loadedArtifacts);
                } else {
                    for (FileInfo fileInfo : loadedArtifacts) {
                        File processedFile = preprocessFile(domainConfig, fileInfo.getFile(), domainConfig.getArtifactInfo().get(validationType).get(artifactTypeToUse).getPreProcessorPath(), domainConfig.getArtifactInfo().get(validationType).get(artifactTypeToUse).getPreProcessorOutputExtension());
                        files.add(new FileInfo(processedFile));
                    }
                }
            }
            preconfiguredLocalArtifactMap.put(key, files);
        }
        List<FileInfo> allFiles = new ArrayList<>(preconfiguredLocalArtifactMap.get(key));
        if (domainConfig.hasRemoteArtifacts(validationType, artifactTypeToUse)) {
            // Remote files - these are already pre-processed as part of the bootstrap.
            preconfiguredRemoteArtifactMap.computeIfAbsent(key, k -> {
                logger.warn("Remote validation artifacts were not found to be cached. This is not normal as caching is done via the periodic reload.");
                return getRemoteValidationArtifacts(domainConfig, validationType, artifactTypeToUse);
            });
            allFiles.addAll(preconfiguredRemoteArtifactMap.get(key));
            // Check to see if we should stop the process due to remote artefact load failures.
            if (!domainConfig.checkRemoteArtefactStatus(validationType) && domainConfig.getResponseForRemoteArtefactLoadFailure(validationType) == ErrorResponseTypeEnum.FAIL) {
                throw new ValidatorException("validator.label.exception.failureToLoadRemoteArtefactsError");
            }
        }
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
        String localFolderConfigValue = domainConfig.getArtifactInfo().get(validationType).get(Objects.toString(artifactType, TypedValidationArtifactInfo.DEFAULT_TYPE)).getLocalPath();
        if (StringUtils.isNotEmpty(localFolderConfigValue)) {
            String[] localFiles = StringUtils.split(localFolderConfigValue, ',');
            for (String localFile: localFiles) {
                try {
                    if (config.isRestrictResourcesToDomain()) {
                        if ((new File(localFile.trim())).isAbsolute() || !domainConfigCache.isInDomainFolder(domainConfig.getDomain(), localFile)) {
                            throw new ValidatorException("validator.label.exception.fileOutsideDomain", localFile);
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
                    throw new ValidatorException("validator.label.exception.unableToReadLocalValidationArtefacts", e);
                }
            }
        }
        return localFileReferences;
    }

    /**
     * Get the validation artifacts corresponding to the provided file or folder. If this is a file then the artifact is the
     * file itself, otherwise if it is a folder it is all the files contained within the folder (top-level only).
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
                fileInfo.add(new FileInfo(fileOrFolder, getContentTypeForFile(fileOrFolder)));
            } else {
                // All files are loaded are processed.
                File[] files = fileOrFolder.listFiles();
                if (files != null) {
                    for (File currentFile: files) {
                        if (currentFile.isFile() && isAcceptedArtifactFile(currentFile, artifactType)) {
                            fileInfo.add(new FileInfo(currentFile, getContentTypeForFile(currentFile)));
                        }
                    }
                }
            }
        } else {
            throw new ValidatorException("validator.label.exception.unableToFindValidationArtefact", ((fileOrFolder != null)? fileOrFolder.getPath(): ""));
        }
        return fileInfo;
    }


    /**
     * Get the list of validation artifacts for the provided domain, validation type and artifact type that are loaded from
     * remote resources (i.e. provided as URLs).
     * <p>
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
     * @param httpVersion The HTTP version to use.
     * @return The list of artifacts to use.
     */
    public List<FileInfo> getExternalValidationArtifacts(DomainConfig domainConfig, String validationType, String artifactType, File parentFolder, List<FileContent> externalArtifactContents, HttpClient.Version httpVersion) {
        List<FileInfo> externalArtifactFiles = new ArrayList<>();
        if (externalArtifactContents != null && !externalArtifactContents.isEmpty()) {
            File externalArtifactFolder = new File(parentFolder, UUID.randomUUID().toString());
            for (FileContent externalArtifact: externalArtifactContents) {
                FileInfo file = storeFileContent(externalArtifactFolder, externalArtifact, artifactType, httpVersion);
                externalArtifactFiles.add(file);
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
        for (DomainConfig domainConfig: domainConfigCache.getAllDomainConfigurations()) {
            getExternalDomainFileCacheLock(domainConfig.getDomainName());
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
        return externalDomainFileCacheLocks.computeIfAbsent(domainName, key -> new ReentrantReadWriteLock());
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
        for (DomainConfig domainConfig: domainConfigCache.getAllDomainConfigurations()) {
            if (domainConfig.hasRemoteArtifacts()) {
                ReentrantReadWriteLock domainLock = null;
                try {
                    // Get write lock for domain.
                    logger.debug("Waiting for lock to reset cache for [{}]", domainConfig.getDomainName());
                    domainLock = getExternalDomainFileCacheLock(domainConfig.getDomainName());
                    domainLock.writeLock().lock();
                    logger.debug("Locked cache for [{}]", domainConfig.getDomainName());
                    for (String validationType: domainConfig.getType()) {
                        refreshFileCacheForValidationType(domainConfig, validationType);
                    }
                } catch (ValidatorException e) {
                    // Never allow configuration errors in one domain to prevent the others from being available.
                    logger.error("Error while processing configuration for domain [{}]: {}", domainConfig.getDomainName(), e.getMessageForLog(), e);
                } catch (Exception e) {
                    // Never allow configuration errors in one domain to prevent the others from being available.
                    logger.error("Error while processing configuration for domain [{}]", domainConfig.getDomainName(), e);
                } finally {
                    // Unlock domain.
                    if (domainLock != null) {
                        domainLock.writeLock().unlock();
                    }
                    logger.debug("Reset remote validation artifact cache for [{}]", domainConfig.getDomainName());
                }
            }
        }
    }

    /**
     * Refresh the remote artefacts for the given validation type.
     *
     * @param domainConfig The domain configuration.
     * @param validationType The validation type to process.
     */
    private void refreshFileCacheForValidationType(DomainConfig domainConfig, String validationType) {
        boolean downloadsSucceeded = true;
        try {
            // Empty cache folder.
            File remoteConfigFolder = new File(new File(getRemoteFileCacheFolder(), domainConfig.getDomainName()), validationType);
            FileUtils.deleteQuietly(remoteConfigFolder);
            TypedValidationArtifactInfo typedArtifactInfo = domainConfig.getArtifactInfo().get(validationType);
            for (String artifactType: typedArtifactInfo.getTypes()) {
                if (!downloadRemoteFilesForArtifactType(artifactType, validationType, remoteConfigFolder, domainConfig, typedArtifactInfo)) {
                    downloadsSucceeded = false;
                }
            }
        } finally {
            domainConfig.setRemoteArtefactStatus(validationType, downloadsSucceeded);
        }
    }

    /**
     * Process the remote files for a given artifact type ensuring that errors do not propagate.
     *
     * @param artifactType The artifact type.
     * @param validationType The validation type.
     * @param remoteConfigFolder The remote config folder.
     * @param domainConfig The domain config.
     * @param typedArtifactInfo The artifact info.
     * @return Whether all downloads succeeded.
     */
    private boolean downloadRemoteFilesForArtifactType(String artifactType, String validationType, File remoteConfigFolder, DomainConfig domainConfig, TypedValidationArtifactInfo typedArtifactInfo) {
        var result = false;
        try {
            artifactType = Objects.toString(artifactType, TypedValidationArtifactInfo.DEFAULT_TYPE);
            File remoteFolderForType = new File(remoteConfigFolder, artifactType);
            downloadRemoteFiles(domainConfig.getDomain(), typedArtifactInfo.get(artifactType).getRemoteArtifacts(), remoteFolderForType, artifactType, domainConfig.getHttpVersion());
            // Update cache map.
            String key = domainConfig.getDomainName() + "|" + validationType + "|" + Objects.toString(artifactType, TypedValidationArtifactInfo.DEFAULT_TYPE);
            preconfiguredRemoteArtifactMap.put(key, getRemoteValidationArtifacts(domainConfig, validationType, artifactType));
            result = true;
        } catch (ValidatorException e) {
            // Never allow configuration errors in one validation type to prevent the others from being available.
            logger.error("Error while processing configuration for type [{}] of domain [{}]: {}", validationType, domainConfig.getDomainName(), e.getMessageForLog(), e);
        } catch (Exception e) {
            // Never allow configuration errors in one validation type to prevent the others from being available.
            logger.error("Error while processing configuration for type [{}] of domain [{}]", validationType, domainConfig.getDomainName(), e);
        }
        return result;
    }

    /**
     * Download and store the provided set of remote validation artifacts.
     *
     * @param domain The domain to consider.
     * @param remoteFiles The list of information per remote file to be loaded.
     * @param remoteConfigPath The folder in which to store the resulting files.
     * @param artifactType The artifact type in question.
     * @param httpVersion The HTTP version to use.
     * @throws IOException If a processing error occurs.
     */
    private void downloadRemoteFiles(String domain, List<RemoteValidationArtifactInfo> remoteFiles, File remoteConfigPath, String artifactType, HttpClient.Version httpVersion) throws IOException {
        if (remoteFiles != null) {
            for (RemoteValidationArtifactInfo artifactInfo: remoteFiles) {
                File preprocessorFile = null;
                if (artifactInfo.getPreProcessorPath() != null) {
                    preprocessorFile = Paths.get(config.getResourceRoot(), domain, artifactInfo.getPreProcessorPath()).toFile();
                }
                getFileFromURL(remoteConfigPath, artifactInfo.getUrl(), null, null, preprocessorFile, artifactInfo.getPreProcessorOutputExtension(), artifactType, StringUtils.isEmpty(artifactInfo.getType())?null:List.of(artifactInfo.getType()), httpVersion);
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
     * @param isAggregate Whether the report is an aggregate.
     * @param <R> The specific type of domain configuration subclass.
     */
    public <R extends DomainConfig> void saveReport(TAR report, String uuid, R domainConfig, boolean isAggregate) {
        File outputFile = new File(getReportFolder(), "TAR-"+uuid+(isAggregate?"_aggregate":"")+".xml");
        this.saveReport(report, outputFile, domainConfig);
    }

    /**
     * Save the provided TAR report in the validator's temporary file system as a detailed TAR report.
     *
     * @param report The report to serialise and persist.
     * @param uuid The identifier to use when constructing the file's name.
     * @param domainConfig The domain's configuration.
     * @param <R> The specific type of domain configuration subclass.
     */
    public <R extends DomainConfig> void saveReport(TAR report, String uuid, R domainConfig) {
        this.saveReport(report, uuid, domainConfig, false);
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
            outputFile.getParentFile().mkdirs();
            try (OutputStream fos = new FileOutputStream(outputFile)) {
                saveReport(report, fos, domainConfig);
            }
        } catch(IOException e) {
            logger.warn("Unable to save XML report", e);
        } catch (Exception e) {
            logger.warn("Unable to marshal XML report", e);
        }
    }

    /**
     * Save the provided TAR report to the provided stream.
     *
     * @param report The report to serialise and persist.
     * @param outputStream The stream to write the serialised report to.
     * @param domainConfig The domain's configuration.
     * @param <R> The specific type of domain configuration subclass.
     */
    public <R extends DomainConfig> void saveReport(TAR report, OutputStream outputStream, R domainConfig) {
        saveReport(report, outputStream, domainConfig, true);
    }

    /**
     * Save the provided TAR report to the provided stream.
     *
     * @param report The report to serialise and persist.
     * @param outputStream The stream to write the serialised report to.
     * @param domainConfig The domain's configuration.
     * @param <R> The specific type of domain configuration subclass.
     * @param useCDataForValues Use CDATA blocks for the context values.
     */
    public <R extends DomainConfig> void saveReport(TAR report, OutputStream outputStream, R domainConfig, boolean useCDataForValues) {
        limitReportItemsIfNeeded(report, domainConfig);
        try {
            Marshaller m = REPORT_CONTEXT.createMarshaller();

            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            Document document = docBuilderFactory.newDocumentBuilder().newDocument();
            m.marshal(OBJECT_FACTORY.createTestStepReport(report), document);

            Transformer transformer = Utils.secureTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            if (useCDataForValues) {
                transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "{http://www.gitb.com/core/v1/}value");
            }
            transformer.transform(new DOMSource(document), new StreamResult(outputStream));
            outputStream.flush();
        } catch(IOException e) {
            logger.warn("Unable to save XML report", e);
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
        logger.debug("Signalling validation start for [{}]", domainName);
        getExternalDomainFileCacheLock(domainName).readLock().lock();
        logger.debug("Signalled validation start for [{}]", domainName);
    }

    /**
     * Signal the end of a validation. This releases the locks preventing remote file cache refreshes.
     *
     * @param domainName The name of the domain in question.
     */
    public void signalValidationEnd(String domainName) {
        logger.debug("Signalling validation end for [{}]", domainName);
        getExternalDomainFileCacheLock(domainName).readLock().unlock();
        logger.debug("Signalled validation end for [{}]", domainName);
    }

    /**
     * Store a file based on the provided content.
     *
     * @param targetFolder The folder within which to create the file.
     * @param content The content string (can be a URL, BASE64 string or the content directly).
     * @param embeddingMethod The method determining how the content string is to be handled.
     * @param contentType The type of the content (as a mime type).
     * @param artifactType The artifact type.
     * @param httpVersion The HTTP version to use.
     * @return The stored file.
     */
    public FileInfo storeFileContent(File targetFolder, String content, ValueEmbeddingEnumeration embeddingMethod, String contentType, String artifactType, HttpClient.Version httpVersion) {
        FileInfo file;
        if (content != null) {
            if (embeddingMethod != null) {
                switch (embeddingMethod) {
                    case STRING:
                        // Use string as-is.
                        try {
                            file = new FileInfo(getFileFromString(targetFolder, content, contentType), contentType);
                        } catch (IOException e) {
                            throw new ValidatorException("validator.label.exception.unableToProcessString", e);
                        }
                        break;
                    case URI:
                        // Read the string from the provided URI.
                        FileInfo loadedFile;
                        try {
                            loadedFile = getFileFromURL(targetFolder, content, getFileExtension(contentType), null, null, null, artifactType, StringUtils.isEmpty(contentType)?null:List.of(contentType), httpVersion);
                        } catch (IOException e) {
                            throw new ValidatorException("validator.label.exception.unableToProcessURI", e);
                        }
                        file = new FileInfo(loadedFile.getFile(), getContentTypeForFile(loadedFile, contentType));
                        break;
                    default: // BASE_64
                        // Construct the string from its BASE64 encoded bytes.
                        file = new FileInfo(getFileFromBase64(targetFolder, content, contentType), contentType);
                        break;
                }
            } else {
                FileInfo loadedFile;
                try {
                    loadedFile = getFileFromURLOrBase64(targetFolder, content, contentType, artifactType, httpVersion);
                } catch (IOException e) {
                    throw new ValidatorException("validator.label.exception.unableToSaveContent", e);
                }
                file = new FileInfo(loadedFile.getFile(), getContentTypeForFile(loadedFile, contentType));
            }
        } else {
            throw new ValidatorException("validator.label.exception.unableToSaveEmpty");
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
     * @param httpVersion The HTTP version to use.
     * @return The stored file.
     */
    public FileInfo storeFileContent(File targetFolder, String content, ValueEmbeddingEnumeration embeddingMethod, String artifactType, HttpClient.Version httpVersion) {
        return storeFileContent(targetFolder, content, embeddingMethod, null, artifactType, httpVersion);
    }

    /**
     * Store a file based on the provided content.
     *
     * @param targetFolder The folder within which to create the file.
     * @param content The content's information (can be a URL, BASE64 string or the content directly).
     * @param artifactType The artifact type.
     * @param httpVersion The HTTP version to use.
     * @return The stored file.
     */
    public FileInfo storeFileContent(File targetFolder, FileContent content, String artifactType, HttpClient.Version httpVersion) {
        return storeFileContent(targetFolder, content.getContent(), content.getEmbeddingMethod(), content.getContentType(), artifactType, httpVersion);
    }

}
