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

import java.util.List;
import java.util.Map;

/**
 * Provider of the configuration needed to load plugins.
 *
 * The configuration is currently provided in a map of unique classifiers that point to
 * an array of PluginInfo objects. These represent the plugin JAR and classes to load implementations
 * from.
 */
public interface PluginConfigProvider {

    /**
     * Get the map of plugin classifiers to plugin information (JARs and classes per JAR).
     *
     * Classifiers are unique keys to determine sets of plugins (determined by the caller).
     *
     * @return The plugin configuration map.
     */
    Map<String, List<PluginInfo>> getPluginInfoPerType();

}
