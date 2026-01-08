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

package eu.europa.ec.itb.validation.commons.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import eu.europa.ec.itb.validation.commons.RateLimitPolicy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * Common application-level configuration properties shared by all validators.
 */
public abstract class ApplicationConfig {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);
    public static final String RATE_LIMIT_ENABLED_PROPERTY = "validator.rateLimit.enabled";

    @Autowired
    private Environment env = null;

    private String resourceRoot;
    private String tmpFolder;
    private Set<String> domain;
    private final Map<String, String> domainIdToDomainName = new HashMap<>();
    private final Map<String, String> domainNameToDomainId = new HashMap<>();
    private String startupTimestamp;
    private String resourceUpdateTimestamp;
    private long cleanupWebRate = 300000L;
    private boolean restrictResourcesToDomain = Boolean.TRUE;
    private final RateLimit rateLimit = new RateLimit();
    private final Webhook webhook = new Webhook();
    private String identifier;
    private boolean supportsTestDefinitionInReportItems;
    private boolean supportsAdditionalInformationInReportItems;
    private String baseSoapEndpointUrl;

    /**
     * @return The base public URL for the validator's SOAP endpoints (up to, and without including, the domain).
     */
    public String getBaseSoapEndpointUrl() {
        return baseSoapEndpointUrl;
    }

    /**
     * @param baseSoapEndpointUrl The base public URL for the validator's SOAP endpoints (up to, and without including, the domain).
     */
    public void setBaseSoapEndpointUrl(String baseSoapEndpointUrl) {
        this.baseSoapEndpointUrl = baseSoapEndpointUrl;
    }

    /**
     * @return True if test definitions are supported in validation report items.
     */
    public boolean isSupportsTestDefinitionInReportItems() {
        return supportsTestDefinitionInReportItems;
    }

    /**
     * @param supportsTestDefinitionInReportItems True if test definitions are supported in validation report items.
     */
    public void setSupportsTestDefinitionInReportItems(boolean supportsTestDefinitionInReportItems) {
        this.supportsTestDefinitionInReportItems = supportsTestDefinitionInReportItems;
    }

    /**
     * @return True if additional information is supported in validation report items.
     */
    public boolean isSupportsAdditionalInformationInReportItems() {
        return supportsAdditionalInformationInReportItems;
    }

    /**
     * @param supportsAdditionalInformationInReportItems True if additional information is supported in validation report items.
     */
    public void setSupportsAdditionalInformationInReportItems(boolean supportsAdditionalInformationInReportItems) {
        this.supportsAdditionalInformationInReportItems = supportsAdditionalInformationInReportItems;
    }

    /**
     * @return The rate (in milliseconds) at which temporary web resources are cleaned up.
     */
    public long getCleanupWebRate() {
        return cleanupWebRate;
    }

    /**
     * @param cleanupWebRate The rate (in milliseconds) at which temporary web resources are cleaned up.
     */
    public void setCleanupWebRate(long cleanupWebRate) {
        this.cleanupWebRate = cleanupWebRate;
    }

    /**
     * @return The complete filesystem path from which domain folders are to be loaded.
     */
    public String getResourceRoot() {
        return resourceRoot;
    }

    /**
     * @param resourceRoot The complete filesystem path from which domain folders are to be loaded.
     */
    public void setResourceRoot(String resourceRoot) {
        this.resourceRoot = resourceRoot;
    }

    /**
     * @return The complete file system path for the validator's temporary work folder.
     */
    public String getTmpFolder() {
        return tmpFolder;
    }

    /**
     * @param tmpFolder The complete file system path for the validator's temporary work folder.
     */
    public void setTmpFolder(String tmpFolder) {
        this.tmpFolder = tmpFolder;
    }

    /**
     * @return A set of folder names under the resource root from which domain configurations will be loaded.
     */
    public Set<String> getDomain() {
        return domain;
    }

    /**
     * @param domain A set of folder names under the resource root from which domain configurations will be loaded.
     */
    public void setDomain(Set<String> domain) {
        this.domain = domain;
    }

    /**
     * @return The map mapping domain identifiers (folder names) to public domain names.
     */
    public Map<String, String> getDomainIdToDomainName() {
        return domainIdToDomainName;
    }

    /**
     * @return The map mapping public domain names to domain identifiers (folder names).
     */
    public Map<String, String> getDomainNameToDomainId() {
        return domainNameToDomainId;
    }

    /**
     * @return The validator's startup timestamp.
     */
    public String getStartupTimestamp() {
        return startupTimestamp;
    }

    /**
     * @return The timestamp for the validation resources' last modification.
     */
    public String getResourceUpdateTimestamp() {
        return resourceUpdateTimestamp;
    }

    /**
     * @param restrictResourcesToDomain True if a domain's files are loaded strictly from within the domain's folder.
     */
    public void setRestrictResourcesToDomain(boolean restrictResourcesToDomain) {
    	this.restrictResourcesToDomain = restrictResourcesToDomain;
    }

    /**
     * @return True if a domain's files are loaded strictly from within the domain's folder.
     */
    public boolean isRestrictResourcesToDomain() {
    	return this.restrictResourcesToDomain;
    }

    /**
     * @return Configuration for validation rate limiting.
     */
    public RateLimit getRateLimit() {
        return rateLimit;
    }

    /**
     * @return Configuration on the statistics collection webhook.
     */
    public Webhook getWebhook() {
    	return this.webhook;
    }

    /**
     * @return An identifier for the validator application instance.
     */
    public String getIdentifier() {
    	return this.identifier;
    }

    /**
     * @param identifier An identifier for the validator application instance.
     */
    public void setIdentifier(String identifier) {
    	this.identifier = identifier;
    }

    /**
     * Initialisation method to be called when creating subclasses of this class.
     */
    protected void init() {
        if (resourceRoot != null) {
            if (!Files.isDirectory(Paths.get(resourceRoot))) {
                throw new IllegalStateException("When provided, the validator.resourceRoot property must point to an existing directory");
            }
            // Setup domain.
            if (domain == null || domain.isEmpty()) {
                File[] directories = new File(resourceRoot).listFiles(File::isDirectory);
                if (directories == null || directories.length == 0) {
                    throw new IllegalStateException("The resource root directory ["+resourceRoot+"] is empty");
                }
                domain = Arrays.stream(directories).map(File::getName).collect(Collectors.toSet());
            }
        } else {
            try {
                File tempFolder = Files.createTempDirectory("temp").toFile();
                Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.deleteQuietly(tempFolder)));
                try (var in = Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream(getGenericConfigPath())) {
                    FileUtils.copyInputStreamToFile(Objects.requireNonNull(in), Path.of(tempFolder.getAbsolutePath(), "any", "config.properties").toFile());
                }
                System.setProperty("validator.resourceRoot", tempFolder.toPath().toAbsolutePath().toString());
                File[] directories = new File(tempFolder.getAbsolutePath()).listFiles(File::isDirectory);
                if (directories == null || directories.length == 0) {
                    throw new IllegalStateException("The resource root directory ["+tempFolder+"] is empty");
                }
                domain = Arrays.stream(directories).map(File::getName).collect(Collectors.toSet());
                resourceRoot = tempFolder.getAbsolutePath();
            } catch (java.io.IOException e){
                throw new IllegalStateException("The validator.resourceRoot property was not set and an error prevented using the validator's generic 'any' configuration", e);
            }
        }
        logger.info("Loaded validation domains: {}", domain);
        StringBuilder logMsg = new StringBuilder();
        for (String domainFolder: domain) {
            String domainName = StringUtils.defaultIfBlank(env.getProperty("validator.domainName."+domainFolder), domainFolder);
            this.domainIdToDomainName.put(domainFolder, domainName);
            this.domainNameToDomainId.put(domainName, domainFolder);
            logMsg.append('[').append(domainFolder).append("]=[").append(domainName).append("]");
        }
        logger.info("Loaded validation domain names: {}", logMsg);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss (XXX)");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss (XXX)");
        startupTimestamp = dtf.format(ZonedDateTime.now());
        resourceUpdateTimestamp = sdf.format(new Date(Paths.get(resourceRoot).toFile().lastModified()));
    }

    /**
     * Returns a generic configuration path. This is expected to overriden by individual validator implementations
     * depending on the location of their generic configuration in the classpath.
     *
     * @return The configuration path on the classpath.
     */
    protected String getGenericConfigPath() {
        return "any/config.properties";
    }

    /**
     * Class to hold the validation rate limiting configuration.
     */
    public static class RateLimit {

        private boolean enabled;
        private Map<RateLimitPolicy, Long> capacity;
        private boolean warnOnly;
        private String ipHeader;

        /**
         * @return Whether rate limiting is enabled.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * @param enabled Whether rate limiting is enabled.
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * @return The rate capacities per type of endpoint (policy).
         */
        public Map<RateLimitPolicy, Long> getCapacity() {
            return capacity;
        }

        /**
         * @param capacity The rate capacities per type of endpoint (policy).
         */
        public void setCapacity(Map<RateLimitPolicy, Long> capacity) {
            this.capacity = capacity;
        }

        /**
         * @return Whether hitting the rate limit results only in a warning as opposed to blocking the request.
         */
        public boolean isWarnOnly() {
            return warnOnly;
        }

        /**
         * @param warnOnly Whether hitting the rate limit results only in a warning as opposed to blocking the request.
         */
        public void setWarnOnly(boolean warnOnly) {
            this.warnOnly = warnOnly;
        }

        /**
         * @return The name of an HTTP header to read the client's IP address from.
         */
        public String getIpHeader() {
            return ipHeader;
        }

        /**
         * @param ipHeader The name of an HTTP header to read the client's IP address from.
         */
        public void setIpHeader(String ipHeader) {
            this.ipHeader = ipHeader;
        }
    }

    /**
     * Private class for the webhook config properties.
     */
    public static class Webhook {
    	
    	private String statistics;
    	
    	private String statisticsSecret;

        private boolean statisticsEnableCountryDetection;

        private String statisticsCountryDetectionDbFile;

        private String ipHeader;

        /**
         * @param statistics The webhook URL.
         */
    	public void setStatistics(String statistics) {
    		this.statistics = statistics;
    	}

        /**
         * @return The webhook URL.
         */
    	public String getStatistics() {
    		return this.statistics;
    	}

        /**
         * @param statisticsSecret The secret to be passed as a URL parameter on the webhook calls.
         */
    	public void setStatisticsSecret(String statisticsSecret) {
    		this.statisticsSecret = statisticsSecret;
    	}

        /**
         * @return The secret to be passed as a URL parameter on the webhook calls.
         */
    	public String getStatisticsSecret() {
    		return this.statisticsSecret;
    	}

        /**
         * @param statisticsEnableCountryDetection True if the country code should be extracted form the client's IP
         *                                         address and included in the webhook call payload.
         */
        public void setStatisticsEnableCountryDetection(boolean statisticsEnableCountryDetection){
            this.statisticsEnableCountryDetection = statisticsEnableCountryDetection;
        }

        /**
         * @return True if the country code should be extracted form the client's IP address and included in the webhook
         * call payload.
         */
        public boolean isStatisticsEnableCountryDetection(){
            return this.statisticsEnableCountryDetection;
        }

        /**
         * @param statisticsCountryDetectionDbFile The complete filesystem path to load the MaxMind DB file from from country code
         *                                         extraction.
         */
        public void setStatisticsCountryDetectionDbFile(String statisticsCountryDetectionDbFile){
            this.statisticsCountryDetectionDbFile = statisticsCountryDetectionDbFile;
        }

        /**
         * @return The complete filesystem path to load the MaxMind DB file from from country code extraction.
         */
        public String getStatisticsCountryDetectionDbFile(){
            return this.statisticsCountryDetectionDbFile;
        }

        /**
         * @return The HTTP header to first consult when retrieving the client's IP address.
         */
        public String getIpHeader(){
            return this.ipHeader;
        }

        /**
         * @param ipHeader The HTTP header to first consult when retrieving the client's IP address.
         */
        public void setIpHeader(String ipHeader){
            this.ipHeader = ipHeader;
        }
    }
    
}
