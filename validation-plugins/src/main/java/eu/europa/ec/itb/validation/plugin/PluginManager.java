package eu.europa.ec.itb.validation.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class used to provide plugin support for validators.
 *
 * The job of this class is to load the plugin implementations from the configuration and to provide them to
 * the validator when requested.
 */
@Component
@ConditionalOnBean(PluginConfigProvider.class)
public class PluginManager {

    private static final Logger LOG = LoggerFactory.getLogger(PluginManager.class);

    private final ConcurrentHashMap<String, ValidationPlugin[]> pluginCache = new ConcurrentHashMap<>();
    private final List<URLClassLoader> classLoaders = new ArrayList<>();

    @Autowired
    private PluginConfigProvider configProvider = null;

    /**
     * Load the plugins configured for the validator.
     */
    @PostConstruct
    private void loadPlugins() {
        boolean pluginsFound = false;
        try {
            Map<String, List<PluginInfo>> pluginPathConfigs = configProvider.getPluginInfoPerType();
            for (Map.Entry<String, List<PluginInfo>> pluginConfig: pluginPathConfigs.entrySet()) {
                if (processPluginConfig(pluginConfig.getKey(), pluginConfig.getValue())) {
                    pluginsFound = true;
                }
            }
            if (!pluginsFound) {
                LOG.info("No plugins found");
            }
        } catch (Exception e) {
            LOG.warn("Failed to load plugin configuration. Continuing with no plugins.", e);
        }
    }

    /**
     * Process a configuration entry for plugins.
     *
     * @param cacheKey The key to use for caching.
     * @param pluginConfig The listed config entries.
     * @return Whether plugins were found.
     */
    private boolean processPluginConfig(String cacheKey, List<PluginInfo> pluginConfig) {
        boolean pluginsFound = false;
        try {
            List<ValidationPlugin> plugins = new ArrayList<>();
            for (PluginInfo pluginInfo: pluginConfig) {
                plugins.addAll(getValidatorsFromJar(pluginInfo.getJarPath(), pluginInfo.getPluginClasses().toArray(new String[0])));
            }
            if (!plugins.isEmpty()) {
                pluginsFound = true;
                LOG.info("Loaded {} plugin(s) for {}", plugins.size(), cacheKey);
                pluginCache.put(cacheKey, plugins.toArray(new ValidationPlugin[0]));
            }
        } catch (Exception e) {
            LOG.warn(String.format("Failed to initialise plugins for classifier [%s]. Considering no plugins for this case.", cacheKey), e);
            pluginCache.put(cacheKey, new ValidationPlugin[0]);
        }
        return pluginsFound;
    }

    /**
     * Get the plugins to consider for the given classifier.
     *
     * @param classifier The classifier to determine which plugins to return.
     * @return The array of plugins (never null).
     */
    public ValidationPlugin[] getPlugins(String classifier) {
        ValidationPlugin[] plugins = pluginCache.get(classifier);
        if (plugins == null) {
            plugins = new ValidationPlugin[0];
        }
        return plugins;
    }

    /**
     * Get the plugins from the provided JAR file.
     *
     * @param jarFile The path to the JAR file to read.
     * @param classes The set of fully qualified plugin classes.
     * @return The list of loaded plugins.
     * @throws IllegalStateException If an error occurs when loading plugin classes.
     */
    private List<ValidationPlugin> getValidatorsFromJar(Path jarFile, String[] classes) {
        try {
            List<ValidationPlugin> instances = new ArrayList<>();
            URLClassLoader loader = new URLClassLoader(new URL[] {jarFile.toUri().toURL()}, null);
            classLoaders.add(loader);
            for (String clazz: classes) {
                Class<?> pluginClass = loader.loadClass(clazz);
                instances.add(new PluginAdapter(pluginClass.getConstructor().newInstance(), loader));
            }
            return instances;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to instantiate plugin classes from JAR ["+jarFile.toFile().getAbsolutePath()+"]", e);
        }
    }

    /**
     * Cleanup method to explicitly close plugins' classloaders.
     */
    @PreDestroy
    private void destroy() {
        classLoaders.forEach(loader -> {
            try {
                loader.close();
            } catch (IOException e) {
                // Ignore.
            }
        });
    }
}
