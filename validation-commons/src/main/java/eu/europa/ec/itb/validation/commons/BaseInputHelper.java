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

package eu.europa.ec.itb.validation.commons;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.vs.ValidateRequest;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfigCache;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Base class used as subclass of components to help validators with parsing and validating input.
 *
 * @param <T> The specific file manager class.
 * @param <R> The specific domain config class.
 * @param <Z> The specific application config class.
 */
public abstract class BaseInputHelper<Z extends ApplicationConfig, T extends BaseFileManager<Z>, R extends DomainConfig> {

    @Autowired
    protected T fileManager = null;

    @Autowired
    protected Z appConfig = null;

    @Autowired
    protected DomainConfigCache<R> domainConfigs = null;

    /**
     * Validate and return the embedding method for a given input of a validation call.
     *
     * @param validateRequest The request to process.
     * @param inputName The name of the input for the embedding method.
     * @return The embedding method.
     */
    public ValueEmbeddingEnumeration validateContentEmbeddingMethod(ValidateRequest validateRequest, String inputName){
        List<AnyContent> listContentEmbeddingMethod = Utils.getInputFor(validateRequest, inputName);
        if (!listContentEmbeddingMethod.isEmpty()) {
            AnyContent content = listContentEmbeddingMethod.get(0);
            return getEmbeddingMethod(content);
        } else {
            return null;
        }
    }

    /**
     * Validation of the content.
     *
     * @param validateRequest The request's parameters.
     * @param inputName The name of the input for the content to validate.
     * @param explicitEmbeddingMethod The embedding method provided as an explicit choice.
     * @param contentSyntax The input's content syntax.
     * @param parentFolder The folder within which to create the resulting file.
     * @param httpVersion The HTTP version to use.
     * @return The file to validate.
     */
    public FileInfo validateContentToValidate(ValidateRequest validateRequest, String inputName, ValueEmbeddingEnumeration explicitEmbeddingMethod, String contentSyntax, File parentFolder, HttpClient.Version httpVersion) {
        List<AnyContent> listContentToValidate = Utils.getInputFor(validateRequest, inputName);
        if (!listContentToValidate.isEmpty()) {
            AnyContent content = listContentToValidate.get(0);
            if (explicitEmbeddingMethod == null) {
                explicitEmbeddingMethod = content.getEmbeddingMethod();
            }
            if (explicitEmbeddingMethod == null) {
                // Embedding method not provided as input nor as parameter.
                throw new ValidatorException("validator.label.exception.embeddingMethodEitherAsInputOrAttribute", inputName);
            }
            String valueToProcess = content.getValue();
            if (content.getEmbeddingMethod() == ValueEmbeddingEnumeration.BASE_64 && explicitEmbeddingMethod != ValueEmbeddingEnumeration.BASE_64) {
                // This is a URI or a plain text string encoded as BASE64.
                valueToProcess = new String(Utils.decodeBase64String(valueToProcess, true));
            }
            return validateContentToValidate(valueToProcess, explicitEmbeddingMethod, contentSyntax, parentFolder, httpVersion);
        } else {
            throw new ValidatorException("validator.label.exception.noContentProvided", inputName);
        }
    }

    /**
     * Validation of the content.
     *
     * @param value The content to validate.
     * @param explicitEmbeddingMethod The embedding method provided as an explicit choice used to determine the handling approach for the provided content.
     * @param contentSyntax The input's content syntax.
     * @param parentFolder The folder within which to create the resulting file.
     * @param httpVersion The HTTP version to use.
     * @return The file to validate.
     */
    public FileInfo validateContentToValidate(String value, ValueEmbeddingEnumeration explicitEmbeddingMethod, String contentSyntax, File parentFolder, HttpClient.Version httpVersion) {
        return fileManager.storeFileContent(parentFolder, value, explicitEmbeddingMethod, contentSyntax, null, httpVersion);
    }

    /**
     * Extract the validation type for SOAP validation calls.
     *
     * @param validateRequest The SOAP request.
     * @param inputName The input name to look for.
     * @return The validation type.
     */
    private String extractValidationType(ValidateRequest validateRequest, String inputName) {
        List<AnyContent> listValidationType = Utils.getInputFor(validateRequest, inputName);
        String validationType = null;
        if (!listValidationType.isEmpty()) {
            AnyContent content = listValidationType.get(0);
            if (content.getEmbeddingMethod() == ValueEmbeddingEnumeration.STRING) {
                validationType = content.getValue();
            } else {
                throw new ValidatorException("validator.label.exception.stringEmbeddingMethodForValidationType", content.getEmbeddingMethod());
            }
        }
        return validationType;
    }

    /**
     * Validation of the validation type linked to the specific request.
     *
     * @param requestedDomain The requested domain.
     * @param resolvedDomainConfig The resolved domain's configuration (differs if aliased).
     * @param validateRequest The request's parameters.
     * @param inputName The name of the input from which the validation type will be read.
     * @return The type of validation.
     */
    public String validateValidationType(String requestedDomain, R resolvedDomainConfig, ValidateRequest validateRequest, String inputName) {
        String requestedValidationType = extractValidationType(validateRequest, inputName);
        String validationTypeToUse = determineValidationType(requestedValidationType, requestedDomain, resolvedDomainConfig);
        return validateValidationType(resolvedDomainConfig, validationTypeToUse);
    }

    /**
     * Determine the validation type to consider, taking into account domain aliases and default types.
     *
     * @param requestedValidationType The requested type.
     * @param requestedDomain The requested domain.
     * @param resolvedDomainConfig The resolved domain from the requested one (differs if it was aliased).
     * @return The validation type to consider.
     */
    public String determineValidationType(String requestedValidationType, String requestedDomain, DomainConfig resolvedDomainConfig) {
        if (requestedDomain.equals(resolvedDomainConfig.getDomainName())) {
            return requestedValidationType;
        } else {
            // This case means that the requested domain is an alias of the resolved one.
            var requestedDomainConfig = domainConfigs.getConfigForDomainName(requestedDomain, true, false);
            if (requestedValidationType == null) {
                // If no validation type was provided we need to resolve it and find its aliased type on the target domain.
                String defaultValidationType = requestedDomainConfig.getDefaultType();
                if (defaultValidationType == null) {
                    throw new ValidatorException("validator.label.exception.validationTypeMissing", requestedDomainConfig.getDomainName(), String.join(", ", requestedDomainConfig.getType()));
                } else {
                    requestedValidationType = defaultValidationType;
                }
            }
            // The validation type to use will either be the one defined by an alias or the same type.
            if (requestedDomainConfig.getDomainTypeAlias() == null) {
                return requestedValidationType;
            } else {
                return requestedDomainConfig.getDomainTypeAlias().getOrDefault(requestedValidationType, requestedValidationType);
            }
        }
    }

    /**
     * Validation of the validation type linked to the specific request.
     *
     * @param domainConfig The domain's configuration.
     * @param validationType The validation type to check.
     * @return The type of validation (validated).
     */
    public String validateValidationType(R domainConfig, String validationType) {
        String validationTypeToUse = validationType;
        if (validationTypeToUse == null) {
            String defaultValidationType = domainConfig.getDefaultType();
            if (defaultValidationType == null) {
                throw new ValidatorException("validator.label.exception.validationTypeMissing", domainConfig.getDomainName(), String.join(", ", domainConfig.getType()));
            }
            validationTypeToUse = defaultValidationType;
        } else {
            if (!domainConfig.getType().contains(validationTypeToUse)) {
                // Check also whether the validation type is a configured alias.
                validationTypeToUse = domainConfig.resolveAlias(validationTypeToUse);
                if (validationTypeToUse == null) {
                    throw new ValidatorException("validator.label.exception.validationTypeInvalid", validationType, domainConfig.getDomainName(), String.join(", ", domainConfig.getType()));
                }
            }
        }
        return validationTypeToUse;
    }

    /**
     * Parse the validation request to store and extract the external (user-provided) validation artifacts.
     *
     * @param validateRequest The request's parameters.
     * @param artifactContainerInputName The name of the input that records the set of information for an artifact.
     * @param artifactContentInputName The name of the input for an artifact's content.
     * @param artifactEmbeddingMethodInputName The name of the input for an artifacts input identifying an explicit embedding method.
     * @return The list of extracted and stored files.
     */
    public List<FileContent> toExternalArtifactContents(ValidateRequest validateRequest, String artifactContainerInputName, String artifactContentInputName, String artifactEmbeddingMethodInputName) {
        List<AnyContent> listInput = Utils.getInputFor(validateRequest, artifactContainerInputName);
        if (!listInput.isEmpty()) {
            return toExternalArtifactContents(listInput.get(0), artifactContentInputName, artifactEmbeddingMethodInputName);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Parse the validation request to store and extract the external (user-provided) validation artifacts.
     *
     * @param containerContent The information linked to an externally provided artifact.
     * @param artifactContentInputName The name of the input for an artifact's content.
     * @param artifactEmbeddingMethodInputName The name of the input for an artifacts input identifying an explicit embedding method.
     * @return The list of extracted and stored files.
     */
    public List<FileContent> toExternalArtifactContents(AnyContent containerContent, String artifactContentInputName, String artifactEmbeddingMethodInputName) {
        List<FileContent> filesContent = new ArrayList<>();
        collectExternalArtifactContents(containerContent.getItem(), artifactContentInputName, artifactEmbeddingMethodInputName, filesContent);
        return filesContent;
    }

    /**
     * Internal method to store the external (user-provided) validation artifacts.
     *
     * @param items The input items to consider.
     * @param artifactContentInputName The name of the input for the artifact's content.
     * @param artifactEmbeddingMethodInputName The name of the input for the artifact's explicit embedding method.
     * @param results The set of collected results. This is provided initially empty and completed through this method.
     */
    private void collectExternalArtifactContents(List<AnyContent> items, String artifactContentInputName, String artifactEmbeddingMethodInputName, List<FileContent> results) {
        List<AnyContent> contentItems = new ArrayList<>();
        List<AnyContent> otherItems = new ArrayList<>();
        ValueEmbeddingEnumeration explicitEmbeddingMethod = null;
        for (var item: items) {
            if (!item.getItem().isEmpty()) {
                collectExternalArtifactContents(item.getItem(), artifactContentInputName, artifactEmbeddingMethodInputName, results);
            }
            if (Strings.CS.equals(item.getName(), artifactContentInputName)) {
                contentItems.add(item);
            } else if (Strings.CS.equals(item.getName(), artifactEmbeddingMethodInputName)) {
                explicitEmbeddingMethod = getEmbeddingMethod(item);
            } else {
                otherItems.add(item);
            }
        }
        for (var contentItem: contentItems) {
            var fileContent = new FileContent();
            if (contentItem.getEmbeddingMethod() == ValueEmbeddingEnumeration.BASE_64 && explicitEmbeddingMethod != null && explicitEmbeddingMethod != ValueEmbeddingEnumeration.BASE_64) {
                // This is a URI or a plain text string encoded as BASE64.
                fileContent.setContent(new String(Utils.decodeBase64String(contentItem.getValue(), true)));
            } else {
                fileContent.setContent(contentItem.getValue());
            }
            fileContent.setEmbeddingMethod((explicitEmbeddingMethod == null)?contentItem.getEmbeddingMethod():explicitEmbeddingMethod);
            // Do any additional data extraction needed.
            for (var otherItem: otherItems) {
                populateFileContentFromInput(fileContent, otherItem);
            }
            // Add to results.
            results.add(fileContent);
        }
    }

    /**
     * Method used to drive the extraction of external (user-provided) validation artifacts from a given set of inputs.
     * <p>
     * This method allows specific validators to add additional processing of such artifacts via subclass extensions.
     *
     * @param containerContent The input that contains all information linked to a given artifact.
     * @param domainConfig The domain's configuration.
     * @param validationType The validation type.
     * @param artifactType The artifact type.
     * @param artifactContentInputName The name of the input for the artifact's content.
     * @param artifactEmbeddingMethodInputName The name of the input for the artifact's explicit embedding method.
     * @param parentFolder The folder within which to store the loaded files.
     * @return The list of stored files.
     */
    public List<FileInfo> getExternalArtifactInfo(AnyContent containerContent, R domainConfig, String validationType, String artifactType, String artifactContentInputName, String artifactEmbeddingMethodInputName, File parentFolder) {
        return fileManager.getExternalValidationArtifacts(domainConfig, validationType, artifactType, parentFolder, toExternalArtifactContents(containerContent, artifactContentInputName, artifactEmbeddingMethodInputName), domainConfig.getHttpVersion());
    }

    /**
     * Validate, store and return the external (user-provided) validation artifacts.
     *
     * @param domainConfig The domain's configuration.
     * @param externalArtifacts The list of contents for the validation artifacts to consider.
     * @param validationType The validation type.
     * @param artifactType The artifact type.
     * @param parentFolder The folder within which to store the resulting files.
     * @return The stored files.
     */
    public List<FileInfo> validateExternalArtifacts(R domainConfig, List<FileContent> externalArtifacts, String validationType, String artifactType, File parentFolder) {
        List<FileInfo> artifacts = new ArrayList<>();
        ExternalArtifactSupport support = domainConfig.getArtifactInfo().get(validationType).get(artifactType).getExternalArtifactSupport();
        if (externalArtifacts == null || externalArtifacts.isEmpty()) {
            if (support == ExternalArtifactSupport.REQUIRED) {
                if (artifactType == null) {
                    throw new ValidatorException("validator.label.exception.validationTypeExpectsUserArtefacts", validationType);
                } else {
                    throw new ValidatorException("validator.label.exception.validationTypeExpectsUserArtefactsWithParam", validationType, artifactType);
                }
            }
        } else {
            if (support == ExternalArtifactSupport.NONE) {
                if (artifactType == null) {
                    throw new ValidatorException("validator.label.exception.validationTypeDoesNotExpectUserArtefacts", validationType);
                } else {
                    throw new ValidatorException("validator.label.exception.validationTypeDoesNotExpectUserArtefactsWithParam", validationType, artifactType);
                }
            }
            artifacts = fileManager.getExternalValidationArtifacts(domainConfig, validationType, artifactType, parentFolder, externalArtifacts, domainConfig.getHttpVersion());
        }
        return artifacts;
    }

    /**
     * Validate, store and return the external (user-provided) validation artifacts.
     *
     * @param domainConfig The domain's configuration.
     * @param validateRequest The request's input parameters.
     * @param artifactContainerInputName The name of the input containing an artifact's information.
     * @param artifactContentInputName The name of the input containing the artifact's content.
     * @param artifactEmbeddingMethodInputName The name of the input containing the artifact's explicit embedding method.
     * @param validationType The validation type.
     * @param artifactType The artifact type.
     * @param parentFolder The folder within which to store the resulting files.
     * @return The stored files.
     */
    public List<FileInfo> validateExternalArtifacts(R domainConfig, ValidateRequest validateRequest, String artifactContainerInputName, String artifactContentInputName, String artifactEmbeddingMethodInputName, String validationType, String artifactType, File parentFolder) {
        List<FileContent> artifactContents = toExternalArtifactContents(validateRequest, artifactContainerInputName, artifactContentInputName, artifactEmbeddingMethodInputName);
        return validateExternalArtifacts(domainConfig, artifactContents, validationType, artifactType, parentFolder);
    }

    /**
     * Extension point to populate a file content (i.e. information on a given file) from a specific input. By default no
     * action is performed.
     *
     * @param fileContent The file content to populate.
     * @param inputItem The input item to consider.
     */
    public void populateFileContentFromInput(FileContent fileContent, AnyContent inputItem) {
        // Nothing by default.
    }

    /**
     * Get the embedding method to consider for the provided input.
     *
     * @param content The input.
     * @return The embedding method.
     */
    private ValueEmbeddingEnumeration getEmbeddingMethod(AnyContent content) {
        return getEmbeddingMethod(content.getValue());
    }

    /**
     * Get the embedding method that corresponds to the provided string.
     *
     * @param value The name of a specific embedding method.
     * @return The embedding method.
     */
    public ValueEmbeddingEnumeration getEmbeddingMethod(String value) {
        if (StringUtils.isNotBlank(value)) {
            return FileContent.embeddingMethodFromString(value);
        }
        return null;
    }

    /**
     * Check to see whether external (user-provided) validation artifacts are supported.
     *
     * @param artifactInfoMap The map of information on external artifacts.
     * @param artifactType The artifact type to check for.
     * @return True if external artifacts are indeed supported.
     */
    public boolean supportsExternalArtifacts(Map<String, TypedValidationArtifactInfo> artifactInfoMap, String artifactType) {
        for (TypedValidationArtifactInfo artifactInfo: artifactInfoMap.values()) {
            if (artifactInfo.get(artifactType).getExternalArtifactSupport() != ExternalArtifactSupport.NONE) {
                return true;
            }
        }
        return false;
    }

}
