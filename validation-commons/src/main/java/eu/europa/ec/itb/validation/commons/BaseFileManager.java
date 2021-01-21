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
    @Autowired(required = false)
    protected ArtifactPreprocessor preprocessor = null;

    private ConcurrentHashMap<String, ReadWriteLock> externalDomainFileCacheLocks = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, List<FileInfo>> preconfiguredLocalArtifactMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, List<FileInfo>> preconfiguredRemoteArtifactMap = new ConcurrentHashMap<>();

    public abstract String getFileExtension(String contentType);

    protected String getContentTypeForFile(File file, String declaredContentType) {
        // By default don't try to retrieve the file's type.
        return null;
    }

    protected boolean isAcceptedArtifactFile(File file, String artifactType) {
        // By default accept all files.
        return true;
    }

    public File getWebTmpFolder() {
        return new File(getTempFolder(), "web");
    }

    public File getTempFolder() {
        return new File(config.getTmpFolder());
    }

    public File getRemoteFileCacheFolder() {
        return new File(getTempFolder(), "remote_config");
    }

    public File getFileFromBase64(File targetFolder, String content, String contentType) {
        if (targetFolder == null) {
            targetFolder = getWebTmpFolder();
        }
        /*
        Since the refactoring input processing no longer needs BOM stripping. This is likely because of driving the
        validation using files rather than loading content as a string.

        char[] buffer = new char[1024];
        int numCharsRead;
        StringBuilder sb = new StringBuilder();
        try (BomStrippingReader reader = new BomStrippingReader(new ByteArrayInputStream(Base64.getDecoder().decode(base64Convert)))) {
            while ((numCharsRead = reader.read(buffer, 0, buffer.length)) != -1) {
                sb.append(buffer, 0, numCharsRead);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error when transforming the Base64 into File.", e);
        }

         */
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

    public File getFileFromBase64(File targetFolder, String content) {
        return getFileFromBase64(targetFolder, content, null);
    }

    public File getFileFromURLOrBase64(File targetFolder, String urlOrBase64) throws IOException {
        return getFileFromURLOrBase64(targetFolder, urlOrBase64, null, null);
    }

    public File getFileFromURLOrBase64(File targetFolder, String urlOrBase64, String contentType) throws IOException {
        return getFileFromURLOrBase64(targetFolder, urlOrBase64, contentType, null);
    }

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

    public File getFileFromURL(File targetFolder, String url) throws IOException {
        return getFileFromURL(targetFolder, url, null, null, null, null, null);
    }

    public File getFileFromURL(File targetFolder, String url, String fileName) throws IOException {
        return getFileFromURL(targetFolder, url, null, fileName, null, null, null);
    }

    public File getFileFromURL(File targetFolder, String url, String extension, String fileName) throws IOException {
        return getFileFromURL(targetFolder, url, extension, fileName, null, null, null);
    }

    public File getFileFromURL(File targetFolder, String url, String extension, String fileName, String artifactType) throws IOException {
        return getFileFromURL(targetFolder, url, extension, fileName, null, null, artifactType);
    }

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

    public File getFileFromString(File targetFolder, String content) throws IOException {
        return getFileFromString(targetFolder, content, null, null);
    }

    public File getFileFromString(File targetFolder, String content, String contentType) throws IOException {
        return getFileFromString(targetFolder, content, contentType, null);
    }

    public File getFileFromString(File targetFolder, String content, String contentType, String fileName) throws IOException {
        return getFileFromString(targetFolder, content, contentType, fileName, getFileExtension(contentType));
    }

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

    public File getFileFromInputStream(File targetFolder, InputStream stream, String contentType, String fileName) throws IOException {
        Path tmpPath = createFile(targetFolder, getFileExtension(contentType), fileName);
        Files.copy(stream, tmpPath, StandardCopyOption.REPLACE_EXISTING);
        return tmpPath.toFile();
    }

    public InputStream getInputStreamFromURL(String url) {
        // Read the resource from the provided URI.
        URI uri = URI.create(url);
        Proxy proxy = null;
        List<Proxy> proxies = ProxySelector.getDefault().select(uri);
        if (proxies != null && !proxies.isEmpty()) {
            proxy = proxies.get(0);
        }
        try {
            URLConnection connection;
            if (proxy == null) {
                connection = uri.toURL().openConnection();
            } else {
                connection = uri.toURL().openConnection(proxy);
            }
            return connection.getInputStream();
        } catch (IOException e) {
            throw new ValidatorException("Unable to read provided URI", e);
        }
    }

	public File createTemporaryFolderPath() {
		return createTemporaryFolderPath(getWebTmpFolder());
	}

	public File createTemporaryFolderPath(File parentFolder) {
		UUID folderUUID = UUID.randomUUID();
		Path tmpFolder = Paths.get(parentFolder.getAbsolutePath(), folderUUID.toString());
		return tmpFolder.toFile();
	}

    public Path createFile(File parentFolder) {
        return createFile(parentFolder, null);
    }

    public Path createFile(File parentFolder, String extension) {
        return createFile(parentFolder, extension, null);
    }

    public Path createFile(File parentFolder, String extension, String fileName) {
        if (StringUtils.isBlank(fileName)) {
            fileName = UUID.randomUUID().toString();
        }
        if (extension == null) {
            extension = "";
        } else if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        if (fileName.endsWith(extension)) {
            extension = "";
        }
        Path tmpPath = Paths.get(parentFolder.getAbsolutePath(), fileName + extension);
        tmpPath.toFile().getParentFile().mkdirs();
        return tmpPath;
    }

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

    private File preprocessFile(DomainConfig domainConfig, File fileToProcess, String preProcessorPath, String preProcessorOutputExtension) {
        File preprocessorFile = Paths.get(config.getResourceRoot(), domainConfig.getDomain(), preProcessorPath).toFile();
        return preprocessor.preprocessFile(fileToProcess, preprocessorFile, preProcessorOutputExtension);
    }

    public List<FileInfo> getPreconfiguredValidationArtifacts(DomainConfig domainConfig, String validationType) {
        return getPreconfiguredValidationArtifacts(domainConfig, validationType, null);
    }

    public List<FileInfo> getPreconfiguredValidationArtifacts(DomainConfig domainConfig, String validationType, String artifactType) {
        String key = domainConfig.getDomainName() + "|" + validationType + "|" + StringUtils.defaultString(artifactType, TypedValidationArtifactInfo.DEFAULT_TYPE);
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

    private List<File> getLocalValidationArtifactFiles(DomainConfig domainConfig, String validationType, String artifactType) {
        List<File> localFileReferences = new ArrayList<>();
        String localFolderConfigValue = domainConfig.getArtifactInfo().get(validationType).get(StringUtils.defaultString(artifactType, TypedValidationArtifactInfo.DEFAULT_TYPE)).getLocalPath();
        if (StringUtils.isNotEmpty(localFolderConfigValue)) {
            String[] localFiles = StringUtils.split(localFolderConfigValue, ',');
            for (String localFile: localFiles) {
                localFileReferences.add(Paths.get(config.getResourceRoot(), domainConfig.getDomain(), localFile.trim()).toFile());
            }
        }
        return localFileReferences;
    }

    public List<FileInfo> getLocalValidationArtifacts(File file, String artifactType) {
        List<FileInfo> fileInfo = new ArrayList<>();
        if (file != null && file.exists()) {
            if (file.isFile()) {
                // We are pointing to a single file.
                fileInfo.add(new FileInfo(file, getContentTypeForFile(file, null)));
            } else {
                // All files are loaded are processed.
                File[] files = file.listFiles();
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
        }
        return fileInfo;
    }

    public List<FileInfo> getRemoteValidationArtifacts(DomainConfig domainConfig, String validationType, String artifactType) {
        File remoteConfigFolder = new File(new File(new File(getRemoteFileCacheFolder(), domainConfig.getDomainName()), validationType), artifactType);
        if (remoteConfigFolder.exists()) {
            return getLocalValidationArtifacts(remoteConfigFolder, artifactType);
        } else {
            return Collections.emptyList();
        }
    }

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

    @PostConstruct
    public void init() {
        FileUtils.deleteQuietly(getTempFolder());
        for (Object config: domainConfigCache.getAllDomainConfigurations()) {
            externalDomainFileCacheLocks.put(((DomainConfig)config).getDomainName(), new ReentrantReadWriteLock());
        }
        FileUtils.deleteQuietly(getRemoteFileCacheFolder());
        resetRemoteFileCache();
    }

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

    @Scheduled(fixedDelayString = "${validator.cleanupRate}")
    public void resetRemoteFileCache() {
        logger.debug("Resetting remote validation artifact cache");
        for (Object domainConfigObj: domainConfigCache.getAllDomainConfigurations()) {
            DomainConfig domainConfig = (DomainConfig)domainConfigObj;
            try {
                // Get write lock for domain.
                logger.debug("Waiting for lock to reset cache for ["+domainConfig.getDomainName()+"]");
                externalDomainFileCacheLocks.get(domainConfig.getDomainName()).writeLock().lock();
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
                externalDomainFileCacheLocks.get(domainConfig.getDomainName()).writeLock().unlock();
                logger.debug("Reset remote validation artifact cache for ["+domainConfig.getDomainName()+"]");
            }
        }
    }

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

    public File getReportFolder() {
        return new File(getTempFolder(), "reports");
    }

    public <R extends DomainConfig> void saveReport(TAR report, String uuid, R domainConfig) {
        File outputFile = new File(getReportFolder(), "TAR-"+uuid+".xml");
        saveReport(report, outputFile, domainConfig);
    }

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

    public void signalValidationStart(String domainName) {
        logger.debug("Signalling validation start for ["+domainName+"]");
        externalDomainFileCacheLocks.get(domainName).readLock().lock();
        logger.debug("Signalled validation start for ["+domainName+"]");
    }

    public void signalValidationEnd(String domainName) {
        logger.debug("Signalling validation end for ["+domainName+"]");
        externalDomainFileCacheLocks.get(domainName).readLock().unlock();
        logger.debug("Signalled validation end for ["+domainName+"]");
    }

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

    public File storeFileContent(File targetFolder, String content, ValueEmbeddingEnumeration embeddingMethod, String artifactType) {
        return storeFileContent(targetFolder, content, embeddingMethod, null, artifactType);
    }

    public File storeFileContent(File targetFolder, FileContent content, String artifactType) {
        return storeFileContent(targetFolder, content.getContent(), content.getEmbeddingMethod(), content.getContentType(), artifactType);
    }

}
