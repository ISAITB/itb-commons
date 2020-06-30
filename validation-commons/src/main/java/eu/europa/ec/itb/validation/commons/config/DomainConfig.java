package eu.europa.ec.itb.validation.commons.config;

import eu.europa.ec.itb.validation.commons.ValidatorChannel;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.plugin.PluginInfo;

import java.util.*;

public class DomainConfig {

    private boolean isDefined;
    private String domain;
    private String domainName;
    private List<String> type;
    private Set<ValidatorChannel> channels = new HashSet<>();
    private Map<String, TypedValidationArtifactInfo> artifactInfo;
    // Plugin configuration.
    private List<PluginInfo> pluginDefaultConfig;
    private Map<String, List<PluginInfo>> pluginPerTypeConfig;

    public Map<String, TypedValidationArtifactInfo> getArtifactInfo() {
        return artifactInfo;
    }

    public void setArtifactInfo(Map<String, TypedValidationArtifactInfo> artifactInfo) {
        this.artifactInfo = artifactInfo;
    }

    public List<PluginInfo> getPluginDefaultConfig() {
        return pluginDefaultConfig;
    }

    public void setPluginDefaultConfig(List<PluginInfo> pluginDefaultConfig) {
        this.pluginDefaultConfig = pluginDefaultConfig;
    }

    public Map<String, List<PluginInfo>> getPluginPerTypeConfig() {
        return pluginPerTypeConfig;
    }

    public void setPluginPerTypeConfig(Map<String, List<PluginInfo>> pluginPerTypeConfig) {
        this.pluginPerTypeConfig = pluginPerTypeConfig;
    }

    public boolean hasPlugins(String validationType) {
        return (pluginDefaultConfig != null && !pluginDefaultConfig.isEmpty()) || (pluginPerTypeConfig != null && pluginPerTypeConfig.containsKey(validationType) && !pluginPerTypeConfig.get(validationType).isEmpty());
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public boolean hasMultipleValidationTypes() {
        return type != null && type.size() > 1;
    }

    public List<String> getType() {
        return type;
    }

    public void setType(List<String> type) {
        this.type = type;
    }

    public Set<ValidatorChannel> getChannels() {
        return channels;
    }

    public void setChannels(Set<ValidatorChannel> channels) {
        this.channels = channels;
    }

    public boolean isDefined() {
        return isDefined;
    }

    public void setDefined(boolean defined) {
        isDefined = defined;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Map<String, Map<String, String>> getExternalArtifactInfoMap() {
        // validation type -> artifact type -> support type
        Map<String, Map<String, String>> artifactInfoMap = new HashMap<>();
        if (getArtifactInfo() != null) {
            for (Map.Entry<String, TypedValidationArtifactInfo> entry: getArtifactInfo().entrySet()) {
                Map<String, String> artifactTypeMap = new HashMap<>();
                for (String artifactType: entry.getValue().getTypes()) {
                    String supportType = entry.getValue().get(artifactType).getExternalArtifactSupport().getName();
                    artifactTypeMap.put(artifactType, supportType);
                }
                artifactInfoMap.put(entry.getKey(), artifactTypeMap);
            }
        }
        return artifactInfoMap;
    }

}
