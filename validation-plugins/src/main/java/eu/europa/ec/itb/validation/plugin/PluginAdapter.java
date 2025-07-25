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

import com.gitb.core.AnyContent;
import com.gitb.core.Metadata;
import com.gitb.core.ValidationModule;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tr.*;
import com.gitb.tr.ObjectFactory;
import com.gitb.vs.*;
import com.gitb.vs.Void;

import javax.xml.namespace.QName;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.List;

/**
 * Adapter for plugin implementations to allow them to be called from the current validator.
 * <p>
 * An adapter is needed given that plugins may not always be using the latest version of the
 * GITB types library. In addition, reuse of the current classloader's classes is not possible
 * because we want to enforce isolation between the plugin's classes and those of the validator.
 * <p>
 * The job of this adapter is to wrap the plugin by creating objects and making calls based on
 * reflection. The expected API is that of the ValidationService that should always be backwards
 * compatible. This means that classes and method names are looked up by name as they are not expected
 * to change.
 */
public class PluginAdapter implements ValidationPlugin {

    private static final ObjectFactory TR_OBJECT_FACTORY = new ObjectFactory();

    private final Object internalValidator;
    private final Class<?> classOfValidateRequest;
    private final Class<?> classOfAnyContent;
    private final Class<?> classOfValueEmbeddingEnumeration;
    private final Class<?> classOfVoid;

    /**
     * Constructor.
     *
     * @param internalValidator The ValidationService instance loaded as a plugin.
     * @param pluginClassLoader The classloader linked to the plugin's JAR file.
     */
    protected PluginAdapter(Object internalValidator, ClassLoader pluginClassLoader) {
        this.internalValidator = internalValidator;
        try {
            classOfValidateRequest = pluginClassLoader.loadClass("com.gitb.vs.ValidateRequest");
            classOfAnyContent = pluginClassLoader.loadClass("com.gitb.core.AnyContent");
            classOfValueEmbeddingEnumeration = pluginClassLoader.loadClass("com.gitb.core.ValueEmbeddingEnumeration");
            classOfVoid = pluginClassLoader.loadClass("com.gitb.vs.Void");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Failed to create plugin adapter", e);
        }
    }

    /**
     * Returns the name of the plugin. This is returned as the module's ID or as the name from the module's metadata.
     *
     * @see ValidationPlugin#getModuleDefinition(Void)
     */
    @Override
    public GetModuleDefinitionResponse getModuleDefinition(Void aVoid) {
        try {
            GetModuleDefinitionResponse response = new GetModuleDefinitionResponse();
            response.setModule(new ValidationModule());
            response.getModule().setMetadata(new Metadata());
            Method targetMethod = internalValidator.getClass().getMethod("getModuleDefinition", classOfVoid);
            Object targetResponse = targetMethod.invoke(internalValidator, new Object[] {null});
            if (targetResponse != null) {
                Object targetModule = targetResponse.getClass().getMethod("getModule").invoke(targetResponse);
                if (targetModule != null) {
                    response.getModule().setId((String)targetModule.getClass().getMethod("getId").invoke(targetModule));
                    Object targetMetadata = targetModule.getClass().getMethod("getMetadata").invoke(targetModule);
                    if (targetMetadata != null) {
                        response.getModule().getMetadata().setName((String) targetMetadata.getClass().getMethod("getName").invoke(targetMetadata));
                    }
                }
            }
            return response;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            String message = extractRootErrorMessage(e);
            if (message == null) {
                message = "Failed to call plugin";
            }
            throw new IllegalStateException(message, e);
        }
    }

    /**
     * Extract the root error message from the provided throwable.
     *
     * @param e The error.
     * @return The error message.
     */
    private String extractRootErrorMessage(Throwable e) {
        if (e.getCause() != null) {
            return extractRootErrorMessage(e.getCause());
        } else {
            return e.getMessage();
        }
    }

    /**
     * Convert the value embedding enumeration to prepare it for the plugin call.
     *
     * @param value The value to convert.
     * @return The converted value.
     * @throws NoSuchMethodException For missing methods.
     * @throws InvocationTargetException For errors calling adapted methods.
     * @throws IllegalAccessException The security access issues.
     */
    private Object getAdaptedValueEmbeddingEnumeration(ValueEmbeddingEnumeration value) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return classOfValueEmbeddingEnumeration.getMethod("fromValue", String.class).invoke(null, value.value());
    }

    /**
     * Convert the provided any content instance to prepare it for the plugin call.
     *
     * @param anyContent The any content value to convert.
     * @return The converted object.
     * @throws NoSuchMethodException For missing methods.
     * @throws InvocationTargetException For errors calling adapted methods.
     * @throws IllegalAccessException The security access issues.
     * @throws InstantiationException For errors creating new instances.
     */
    private Object getAdaptedAnyContent(AnyContent anyContent) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Object adaptedAnyContent = null;
        if (anyContent != null) {
            adaptedAnyContent = classOfAnyContent.getConstructor().newInstance();
            classOfAnyContent.getMethod("setValue", String.class).invoke(adaptedAnyContent, anyContent.getValue());
            classOfAnyContent.getMethod("setType", String.class).invoke(adaptedAnyContent, anyContent.getType());
            classOfAnyContent.getMethod("setName", String.class).invoke(adaptedAnyContent, anyContent.getName());
            classOfAnyContent.getMethod("setEncoding", String.class).invoke(adaptedAnyContent, anyContent.getEncoding());
            classOfAnyContent.getMethod("setEmbeddingMethod", classOfValueEmbeddingEnumeration).invoke(adaptedAnyContent, getAdaptedValueEmbeddingEnumeration(anyContent.getEmbeddingMethod()));
            if (!anyContent.getItem().isEmpty()) {
                List<Object> items = (List<Object>) classOfAnyContent.getMethod("getItem").invoke(adaptedAnyContent);
                for (AnyContent item: anyContent.getItem()) {
                    items.add(getAdaptedAnyContent(item));
                }
            }
        }
        return adaptedAnyContent;
    }

    /**
     * Convert the provided validation request to be used for the plugin call.
     *
     * @param validateRequest The request to convert.
     * @return The converted object.
     * @throws NoSuchMethodException For missing methods.
     * @throws InvocationTargetException For errors calling adapted methods.
     * @throws IllegalAccessException The security access issues.
     * @throws InstantiationException For errors creating new instances.
     */
    private Object getAdaptedValidateRequest(ValidateRequest validateRequest) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Object adaptedRequest = classOfValidateRequest.getConstructor().newInstance();
        List<Object> inputs = (List<Object>) classOfValidateRequest.getMethod("getInput").invoke(adaptedRequest);
        for (AnyContent input: validateRequest.getInput()) {
            inputs.add(getAdaptedAnyContent(input));
        }
        return adaptedRequest;
    }

    /**
     * Call the plugin's validate method.
     *
     * @param validateRequest The validation request.
     * @return The response.
     */
    @Override
    public ValidationResponse validate(ValidateRequest validateRequest) {
        try {
            Method validateMethod = internalValidator.getClass().getMethod("validate", classOfValidateRequest);
            Object internalResult = validateMethod.invoke(internalValidator, getAdaptedValidateRequest(validateRequest));
            return toValidationResponse(internalResult);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            String message = extractRootErrorMessage(e);
            if (message == null) {
                message = "Failed to call plugin";
            }
            throw new IllegalStateException(message, e);
        }
    }

    /**
     * Convert the provided validation response from the plugin to return for use in the validator.
     *
     * @param internalResult The plugin result.
     * @return The converted object.
     * @throws NoSuchMethodException For missing methods.
     * @throws InvocationTargetException For errors calling adapted methods.
     * @throws IllegalAccessException The security access issues.
     */
    private ValidationResponse toValidationResponse(Object internalResult) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ValidationResponse response = new ValidationResponse();
        if (internalResult != null) {
            TAR report = new TAR();
            response.setReport(report);
            Object adaptedReport = internalResult.getClass().getMethod("getReport").invoke(internalResult);
            Object adaptedResult = adaptedReport.getClass().getMethod("getResult").invoke(adaptedReport);
            // Value
            report.setResult(TestResultType.valueOf((String)adaptedResult.getClass().getMethod("value").invoke(adaptedResult)));
            // Counters
            Object adaptedCounters = adaptedReport.getClass().getMethod("getCounters").invoke(adaptedReport);
            report.setCounters(new ValidationCounters());
            if (adaptedCounters != null) {
                report.getCounters().setNrOfErrors((BigInteger)adaptedCounters.getClass().getMethod("getNrOfErrors").invoke(adaptedCounters));
                report.getCounters().setNrOfWarnings((BigInteger)adaptedCounters.getClass().getMethod("getNrOfWarnings").invoke(adaptedCounters));
                report.getCounters().setNrOfAssertions((BigInteger)adaptedCounters.getClass().getMethod("getNrOfAssertions").invoke(adaptedCounters));
            }
            if (report.getCounters().getNrOfErrors() == null) {
                report.getCounters().setNrOfErrors(BigInteger.ZERO);
            }
            if (report.getCounters().getNrOfWarnings() == null) {
                report.getCounters().setNrOfWarnings(BigInteger.ZERO);
            }
            if (report.getCounters().getNrOfAssertions() == null) {
                report.getCounters().setNrOfAssertions(BigInteger.ZERO);
            }
            // Report items
            report.setReports(new TestAssertionGroupReportsType());
            Object adaptedReports = adaptedReport.getClass().getMethod("getReports").invoke(adaptedReport);
            if (adaptedReports != null) {
                List<Object> adaptedItems = (List<Object>) adaptedReports.getClass().getMethod("getInfoOrWarningOrError").invoke(adaptedReports);
                if (adaptedItems != null) {
                    for (Object adaptedItem: adaptedItems) {
                        QName adaptedItemName = (QName) adaptedItem.getClass().getMethod("getName").invoke(adaptedItem);
                        Object adaptedItemBAR = adaptedItem.getClass().getMethod("getValue").invoke(adaptedItem);
                        if (adaptedItemName != null && adaptedItemBAR != null && adaptedItemBAR.getClass().getName().equals("com.gitb.tr.BAR")) {
                            BAR bar = new BAR();
                            bar.setDescription((String) adaptedItemBAR.getClass().getMethod("getDescription").invoke(adaptedItemBAR));
                            bar.setType((String) adaptedItemBAR.getClass().getMethod("getType").invoke(adaptedItemBAR));
                            bar.setValue((String) adaptedItemBAR.getClass().getMethod("getValue").invoke(adaptedItemBAR));
                            bar.setLocation((String) adaptedItemBAR.getClass().getMethod("getLocation").invoke(adaptedItemBAR));
                            bar.setTest((String) adaptedItemBAR.getClass().getMethod("getTest").invoke(adaptedItemBAR));
                            bar.setAssertionID((String) adaptedItemBAR.getClass().getMethod("getAssertionID").invoke(adaptedItemBAR));
                            if (adaptedItemName.getLocalPart().equals("error")) {
                                report.getReports().getInfoOrWarningOrError().add(TR_OBJECT_FACTORY.createTestAssertionGroupReportsTypeError(bar));
                            } else if (adaptedItemName.getLocalPart().equals("warning")) {
                                report.getReports().getInfoOrWarningOrError().add(TR_OBJECT_FACTORY.createTestAssertionGroupReportsTypeWarning(bar));
                            } else {
                                report.getReports().getInfoOrWarningOrError().add(TR_OBJECT_FACTORY.createTestAssertionGroupReportsTypeInfo(bar));
                            }
                        }
                    }
                }
            }
        }
        return response;
    }

}
