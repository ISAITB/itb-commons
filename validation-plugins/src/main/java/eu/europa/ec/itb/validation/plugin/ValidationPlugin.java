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

import com.gitb.vs.GetModuleDefinitionResponse;
import com.gitb.vs.ValidationService;

/**
 * Plugin extension of the ValidationService interface.
 */
public interface ValidationPlugin extends ValidationService {

    /**
     * Get the name of the current plugin. The name is looked up first from the
     * module ID and then from the metadata name. If no information is found an empty name
     * is considered and returned.
     *
     * @return The plugin name.
     */
    default String getName() {
        GetModuleDefinitionResponse pluginDefinition = getModuleDefinition(null);
        if (pluginDefinition != null && pluginDefinition.getModule() != null) {
            if (pluginDefinition.getModule().getId() != null) {
                return pluginDefinition.getModule().getId();
            } else if (pluginDefinition.getModule().getMetadata() != null && pluginDefinition.getModule().getMetadata().getName() != null) {
                return pluginDefinition.getModule().getMetadata().getName();
            }
        }
        return "";
    }
}
