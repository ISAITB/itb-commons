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

package eu.europa.ec.itb.validation.plugin;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
 * <p>
 * The job of this class is to load the plugin implementations from the configuration and to provide them to
 * the validator when requested.
 */
@Component
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
            LOG.warn("Failed to initialise plugins for classifier [{}]. Considering no plugins for this case.", cacheKey, e);
            pluginCache.put(cacheKey, new ValidationPlugin[0]);
        }
        return pluginsFound;
    }

    /**
     * @return Whether plugins exist.
     */
    public boolean hasPlugins() {
        return pluginCache.values()
                .stream()
                .anyMatch(plugins -> plugins != null && plugins.length > 0);
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
