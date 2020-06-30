package eu.europa.ec.itb.validation.commons.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public abstract class ApplicationConfig {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);

    @Autowired
    private Environment env = null;

    private String resourceRoot;
    private String tmpFolder;
    private Set<String> domain;
    private Map<String, String> domainIdToDomainName = new HashMap<>();
    private Map<String, String> domainNameToDomainId = new HashMap<>();
    private String startupTimestamp;
    private String resourceUpdateTimestamp;
    private long cleanupWebRate = 300000L;

    public long getCleanupWebRate() {
        return cleanupWebRate;
    }

    public void setCleanupWebRate(long cleanupWebRate) {
        this.cleanupWebRate = cleanupWebRate;
    }

    public String getResourceRoot() {
        return resourceRoot;
    }

    public void setResourceRoot(String resourceRoot) {
        this.resourceRoot = resourceRoot;
    }

    public String getTmpFolder() {
        return tmpFolder;
    }

    public void setTmpFolder(String tmpFolder) {
        this.tmpFolder = tmpFolder;
    }

    public Set<String> getDomain() {
        return domain;
    }

    public void setDomain(Set<String> domain) {
        this.domain = domain;
    }

    public Map<String, String> getDomainIdToDomainName() {
        return domainIdToDomainName;
    }

    public void setDomainIdToDomainName(Map<String, String> domainIdToDomainName) {
        this.domainIdToDomainName = domainIdToDomainName;
    }

    public Map<String, String> getDomainNameToDomainId() {
        return domainNameToDomainId;
    }

    public void setDomainNameToDomainId(Map<String, String> domainNameToDomainId) {
        this.domainNameToDomainId = domainNameToDomainId;
    }

    public String getStartupTimestamp() {
        return startupTimestamp;
    }

    public void setStartupTimestamp(String startupTimestamp) {
        this.startupTimestamp = startupTimestamp;
    }

    public String getResourceUpdateTimestamp() {
        return resourceUpdateTimestamp;
    }

    public void setResourceUpdateTimestamp(String resourceUpdateTimestamp) {
        this.resourceUpdateTimestamp = resourceUpdateTimestamp;
    }

    protected void init() {
        if (resourceRoot != null && Files.isDirectory(Paths.get(resourceRoot))) {
            // Setup domain.
            if (domain == null || domain.isEmpty()) {
                File[] directories = new File(resourceRoot).listFiles(File::isDirectory);
                if (directories == null || directories.length == 0) {
                    throw new IllegalStateException("The resource root directory ["+resourceRoot+"] is empty");
                }
                domain = Arrays.stream(directories).map(File::getName).collect(Collectors.toSet());
            }
        } else {
            throw new IllegalStateException("Invalid resourceRoot configured ["+resourceRoot+"]. Ensure you specify the validator.resourceRoot property correctly.");
        }
        logger.info("Loaded validation domains: "+domain);
        // Load domain names.
        StringBuilder logMsg = new StringBuilder();
        for (String domainFolder: domain) {
            String domainName = StringUtils.defaultIfBlank(env.getProperty("validator.domainName."+domainFolder), domainFolder);
            this.domainIdToDomainName.put(domainFolder, domainName);
            this.domainNameToDomainId.put(domainName, domainFolder);
            logMsg.append('[').append(domainFolder).append("]=[").append(domainName).append("]");
        }
        logger.info("Loaded validation domain names: " + logMsg.toString());
        // Set startup times and resource update times.
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss (XXX)");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss (XXX)");
        startupTimestamp = dtf.format(ZonedDateTime.now());
        resourceUpdateTimestamp = sdf.format(new Date(Paths.get(resourceRoot).toFile().lastModified()));
    }
}
