package eu.europa.ec.itb.validation.plugin;

import java.nio.file.Path;
import java.util.List;

/**
 * The information needed to load a plugin (JAR file and list of class names).
 */
public class PluginInfo {

    private Path jarPath;
    private List<String> pluginClasses;

    /**
     * @return The relative path within the domain folder to load the plugin's JAR file.
     */
    public Path getJarPath() {
        return jarPath;
    }

    /**
     * @param jarPath The relative path within the domain folder to load the plugin's JAR file.
     */
    public void setJarPath(Path jarPath) {
        this.jarPath = jarPath;
    }

    /**
     * @return The list of fully qualified class names to load as plugin implementations.
     */
    public List<String> getPluginClasses() {
        return pluginClasses;
    }

    /**
     * @param pluginClasses The list of fully qualified class names to load as plugin implementations.
     */
    public void setPluginClasses(List<String> pluginClasses) {
        this.pluginClasses = pluginClasses;
    }

}
