/*
 * Copyright (C) 2026 European Union
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
