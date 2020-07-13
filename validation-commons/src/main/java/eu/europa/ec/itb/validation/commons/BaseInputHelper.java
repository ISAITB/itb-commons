package eu.europa.ec.itb.validation.commons;

import com.gitb.core.AnyContent;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.vs.ValidateRequest;
import eu.europa.ec.itb.validation.commons.artifact.ExternalArtifactSupport;
import eu.europa.ec.itb.validation.commons.artifact.TypedValidationArtifactInfo;
import eu.europa.ec.itb.validation.commons.config.ApplicationConfig;
import eu.europa.ec.itb.validation.commons.config.DomainConfig;
import eu.europa.ec.itb.validation.commons.error.ValidatorException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class BaseInputHelper<T extends BaseFileManager, R extends DomainConfig, Z extends ApplicationConfig> {

    @Autowired
    protected T fileManager = null;

    @Autowired
    protected Z appConfig = null;

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
     * @param validateRequest The request's parameters.
     * @param explicitEmbeddingMethod The embedding method.
     * @return The file to validate.
     */
    public File validateContentToValidate(ValidateRequest validateRequest, String inputName, ValueEmbeddingEnumeration explicitEmbeddingMethod, File parentFolder) {
        List<AnyContent> listContentToValidate = Utils.getInputFor(validateRequest, inputName);
        if (!listContentToValidate.isEmpty()) {
            AnyContent content = listContentToValidate.get(0);
            if (explicitEmbeddingMethod == null) {
                explicitEmbeddingMethod = content.getEmbeddingMethod();
            }
            if (explicitEmbeddingMethod == null) {
                // Embedding method not provided as input nor as parameter.
                throw new ValidatorException(String.format("The embedding method needs to be provided either as input parameter or be set as an attribute on the [%s] input.", inputName));
            }
            String valueToProcess = content.getValue();
            if (content.getEmbeddingMethod() == ValueEmbeddingEnumeration.BASE_64 && explicitEmbeddingMethod != ValueEmbeddingEnumeration.BASE_64) {
                // This is a URI or a plain text string encoded as BASE64.
                valueToProcess = new String(java.util.Base64.getDecoder().decode(valueToProcess));
            }
            return validateContentToValidate(valueToProcess, explicitEmbeddingMethod, parentFolder);
        } else {
            throw new ValidatorException(String.format("No content was provided for validation (input parameter [%s]).", inputName));
        }
    }

    public File validateContentToValidate(String value, ValueEmbeddingEnumeration explicitEmbeddingMethod, File parentFolder) {
        return fileManager.storeFileContent(parentFolder, value, explicitEmbeddingMethod, null);
    }

    /**
     * Validation of the mime type of the provided RDF content.
     * @param validateRequest The request's parameters.
     * @return The type of validation.
     */
    public String validateValidationType(R domainConfig, ValidateRequest validateRequest, String inputName) {
        List<AnyContent> listValidationType = Utils.getInputFor(validateRequest, inputName);
        String validationType = null;
        if (!listValidationType.isEmpty()) {
            AnyContent content = listValidationType.get(0);
            if (content.getEmbeddingMethod() == ValueEmbeddingEnumeration.STRING) {
                validationType = content.getValue();
            } else {
                throw new ValidatorException(String.format("The validation type to perform must be provided with a [STRING] embeddingMethod. This was provided as [%s].", content.getEmbeddingMethod()));
            }
        }
        return validateValidationType(domainConfig, validationType);
    }

    public String validateValidationType(R domainConfig, String validationType) {
        if (validationType != null && !domainConfig.getType().contains(validationType)) {
            throw new ValidatorException(String.format("The provided validation type [%s] is not valid for domain [%s]. Available types are [%s].", validationType, domainConfig.getDomainName(), String.join(", ", domainConfig.getType())));
        } else if (validationType == null && domainConfig.getType().size() != 1) {
            throw new ValidatorException(String.format("A validation type must be provided for domain [%s]. Available types are [%s].", domainConfig.getDomainName(), String.join(", ", domainConfig.getType())));
        }
        return validationType==null ? domainConfig.getType().get(0) : validationType;
    }

    public List<FileContent> toExternalArtifactContents(ValidateRequest validateRequest, String artifactContainerInputName, String artifactContentInputName, String artifactEmbeddingMethodInputName) {
        List<AnyContent> listInput = Utils.getInputFor(validateRequest, artifactContainerInputName);
        if (!listInput.isEmpty()) {
            return toExternalArtifactContents(listInput.get(0), artifactContentInputName, artifactEmbeddingMethodInputName);
        } else {
            return Collections.emptyList();
        }
    }

    public List<FileContent> toExternalArtifactContents(AnyContent containerContent, String artifactContentInputName, String artifactEmbeddingMethodInputName) {
        List<FileContent> filesContent = new ArrayList<>();
        FileContent artifactContent = toExternalArtifactContent(containerContent, artifactContentInputName, artifactEmbeddingMethodInputName);
        if (!StringUtils.isEmpty(artifactContent.getContent())) {
            filesContent.add(artifactContent);
        }
        for (AnyContent content: containerContent.getItem()) {
            FileContent fileContent = toExternalArtifactContent(content, artifactContentInputName, artifactEmbeddingMethodInputName);
            if (!StringUtils.isEmpty(fileContent.getContent())) {
                filesContent.add(fileContent);
            }
        }
        return filesContent;
    }

    /*
    This method can be overriden to provide special processing for external artifacts.
     */
    public List<FileInfo> getExternalArtifactInfo(AnyContent containerContent, R domainConfig, String validationType, String artifactType, String artifactContentInputName, String artifactEmbeddingMethodInputName, File parentFolder) {
        return fileManager.getExternalValidationArtifacts(domainConfig, validationType, artifactType, parentFolder, toExternalArtifactContents(containerContent, artifactContentInputName, artifactEmbeddingMethodInputName));
    }

    public List<FileInfo> validateExternalArtifacts(R domainConfig, List<FileContent> externalArtifacts, String validationType, String artifactType, File parentFolder) {
        List<FileInfo> artifacts = new ArrayList<>();
        ExternalArtifactSupport support = domainConfig.getArtifactInfo().get(validationType).get(artifactType).getExternalArtifactSupport();
        if (externalArtifacts == null || externalArtifacts.isEmpty()) {
            if (support == ExternalArtifactSupport.REQUIRED) {
                if (artifactType == null) {
                    throw new ValidatorException(String.format("Validation type [%s] expects user-provided artifacts.", validationType));
                } else {
                    throw new ValidatorException(String.format("Validation type [%s] expects user-provided artifacts (%s).", validationType, artifactType));
                }
            }
        } else {
            if (support == ExternalArtifactSupport.NONE) {
                if (artifactType == null) {
                    throw new ValidatorException(String.format("Validation type [%s] does not expect user-provided artifacts.", validationType));
                } else {
                    throw new ValidatorException(String.format("Validation type [%s] does not expect user-provided artifacts (%s).", validationType, artifactType));
                }
            }
            artifacts = fileManager.getExternalValidationArtifacts(domainConfig, validationType, artifactType, parentFolder, externalArtifacts);
        }
        return artifacts;
    }

    public List<FileInfo> validateExternalArtifacts(R domainConfig, ValidateRequest validateRequest, String artifactContainerInputName, String artifactContentInputName, String artifactEmbeddingMethodInputName, String validationType, String artifactType, File parentFolder) {
        List<FileContent> artifactContents = toExternalArtifactContents(validateRequest, artifactContainerInputName, artifactContentInputName, artifactEmbeddingMethodInputName);
        return validateExternalArtifacts(domainConfig, artifactContents, validationType, artifactType, parentFolder);
    }

    public void populateFileContentFromInput(FileContent fileContent, AnyContent inputItem) {
        // Nothing by default.
    }

    private FileContent toExternalArtifactContent(AnyContent content, String artifactContentInputName, String artifactEmbeddingMethodInputName) {
        FileContent fileContent = new FileContent();
        ValueEmbeddingEnumeration embeddingMethod = null;
        ValueEmbeddingEnumeration explicitEmbeddingMethod = null;
        if (content.getItem() != null && !content.getItem().isEmpty()) {
            boolean isContent = false;
            for (AnyContent inputItem : content.getItem()) {
                if (StringUtils.equals(inputItem.getName(), artifactContentInputName)) {
                    embeddingMethod = inputItem.getEmbeddingMethod();
                    fileContent.setContent(inputItem.getValue());
                    isContent = true;
                }
                if (StringUtils.equals(inputItem.getName(), artifactEmbeddingMethodInputName)) {
                    explicitEmbeddingMethod = getEmbeddingMethod(inputItem);
                }
                // Do any additional data extraction needed.
                populateFileContentFromInput(fileContent, inputItem);
            }
            if (isContent) {
                if (explicitEmbeddingMethod == null) {
                    explicitEmbeddingMethod = embeddingMethod;
                }
                if (explicitEmbeddingMethod == null) {
                    // Embedding method not provided as input nor as parameter.
                    throw new ValidatorException(String.format("For user-provided schemas the embedding method needs to be provided either as a separate input [%s] or as an attribute of the [%s] input.", artifactEmbeddingMethodInputName, artifactContentInputName));
                }
                if (embeddingMethod == ValueEmbeddingEnumeration.BASE_64 && explicitEmbeddingMethod != ValueEmbeddingEnumeration.BASE_64) {
                    // This is a URI or a plain text string encoded as BASE64.
                    fileContent.setContent(new String(java.util.Base64.getDecoder().decode(fileContent.getContent())));
                }
                fileContent.setEmbeddingMethod(explicitEmbeddingMethod);
            }
        }
        return fileContent;
    }

    private ValueEmbeddingEnumeration getEmbeddingMethod(AnyContent content) {
        return getEmbeddingMethod(content.getValue());
    }

    public ValueEmbeddingEnumeration getEmbeddingMethod(String value) {
        if (StringUtils.isNotBlank(value)) {
            return FileContent.embeddingMethodFromString(value);
        }
        return null;
    }

    public boolean supportsExternalArtifacts(Map<String, TypedValidationArtifactInfo> artifactInfoMap, String artifactType) {
        for (TypedValidationArtifactInfo artifactInfo: artifactInfoMap.values()) {
            if (artifactInfo.get(artifactType).getExternalArtifactSupport() != ExternalArtifactSupport.NONE) {
                return true;
            }
        }
        return false;
    }

}
