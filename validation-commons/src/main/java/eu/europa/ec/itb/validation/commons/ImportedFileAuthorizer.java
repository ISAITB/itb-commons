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

package eu.europa.ec.itb.validation.commons;

import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Class used to validate file URIs referenced within validation artefacts.
 */
public class ImportedFileAuthorizer {

    private final Collection<Path> acceptedRootPaths;

    private ImportedFileAuthorizer(Collection<Path> acceptedRootPaths) {
        this.acceptedRootPaths = acceptedRootPaths.stream()
                .map(path -> path.toAbsolutePath().normalize())
                .toList();
    }

    /**
     * Check to see that the provided file URI is allowed.
     *
     * @param uri The URI to check.
     * @return The check result.
     * @throws ValidatorException If the URI is not file-based or the relevant path is not allowed.
     */
    public boolean isUriAllowed(URI uri) {
        if (uri == null) {
            throw new ValidatorException("validator.label.exception.notAllowedToReadImportedPath");
        }
        if (!"file".equalsIgnoreCase(uri.getScheme())) {
            throw new ValidatorException("validator.label.exception.notAllowedToReadImportedPath");
        }
        return isPathAllowed(Path.of(uri));
    }

    /**
     * Check to see that the provided file path is allowed.
     *
     * @param path The path to check.
     * @return The check result.
     * @throws ValidatorException If the path is not allowed.
     */
    public boolean isPathAllowed(Path path) {
        if (path == null) {
            throw new ValidatorException("validator.label.exception.notAllowedToReadImportedPath");
        }
        Path candidate = path.toAbsolutePath().normalize();
        boolean allowed = acceptedRootPaths.stream()
                .anyMatch(candidate::startsWith);
        if (!allowed) {
            throw new ValidatorException("validator.label.exception.notAllowedToReadImportedPath");
        }
        return true;
    }

    /**
     * Construct an authorizer based on the application configuration, domain configuration and specific validation temp folder.
     *
     * @param appConfig The application configuration.
     * @param domainConfig The domain configuration.
     * @param tempFolderForValidation The validation's temporary folder.
     * @return The authorizer to use.
     */
    public static ImportedFileAuthorizer from(ApplicationConfig appConfig, DomainConfig domainConfig, Path tempFolderForValidation) {
        if (appConfig.isRestrictResourcesToDomain()) {
            // Allow referenced files to be either under the temp folder for the specific validation, or under the domain's root folder.
            return new ImportedFileAuthorizer(List.of(tempFolderForValidation, Path.of(appConfig.getTmpFolder()), Path.of(domainConfig.getDomainRoot())));
        } else {
            // Allow referenced files to be either under the temp folder for the specific validation, or under the validator's resource root folder.
            return new ImportedFileAuthorizer(List.of(tempFolderForValidation, Path.of(appConfig.getTmpFolder()), Path.of(appConfig.getResourceRoot())));
        }
    }

}
