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
 *
 * An adapter is needed given that plugins may not always be using the latest version of the
 * GITB types library. In addition, reuse of the current classloader's classes is not possible
 * because we want to enforce isolation between the plugin's classes and those of the validator.
 *
 * The job of this adapter is to wrap the plugin by creating objects and making calls based on
 * reflection. The expected API is that of the ValidationService that should always be backwards
 * compatible. This means that classes and method names are looked up by name as they are not expected
 * to change.
 */
public class PluginAdapter implements ValidationPlugin {

    private final static ObjectFactory TR_OBJECT_FACTORY = new ObjectFactory();

    private final Object internalValidator;
    private final Class<?> classOf_ValidateRequest;
    private final Class<?> classOf_AnyContent;
    private final Class<?> classOf_ValueEmbeddingEnumeration;
    private final Class<?> classOf_Void;

    /**
     * Constructor.
     *
     * @param internalValidator The ValidationService instance loaded as a plugin.
     * @param pluginClassLoader The classloader linked to the plugin's JAR file.
     */
    protected PluginAdapter(Object internalValidator, ClassLoader pluginClassLoader) {
        this.internalValidator = internalValidator;
        try {
            classOf_ValidateRequest = pluginClassLoader.loadClass("com.gitb.vs.ValidateRequest");
            classOf_AnyContent = pluginClassLoader.loadClass("com.gitb.core.AnyContent");
            classOf_ValueEmbeddingEnumeration = pluginClassLoader.loadClass("com.gitb.core.ValueEmbeddingEnumeration");
            classOf_Void = pluginClassLoader.loadClass("com.gitb.vs.Void");
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
            Method targetMethod = internalValidator.getClass().getMethod("getModuleDefinition", classOf_Void);
            Object _response = targetMethod.invoke(internalValidator, new Object[] {null});
            if (_response != null) {
                Object _module = _response.getClass().getMethod("getModule").invoke(_response);
                if (_module != null) {
                    response.getModule().setId((String)_module.getClass().getMethod("getId").invoke(_module));
                    Object _metadata = _module.getClass().getMethod("getMetadata").invoke(_module);
                    if (_metadata != null) {
                        response.getModule().getMetadata().setName((String) _metadata.getClass().getMethod("getName").invoke(_metadata));
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
        return classOf_ValueEmbeddingEnumeration.getMethod("fromValue", String.class).invoke(null, value.value());
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
        Object _anyContent = null;
        if (anyContent != null) {
            _anyContent = classOf_AnyContent.getConstructor().newInstance();
            classOf_AnyContent.getMethod("setValue", String.class).invoke(_anyContent, new Object[] {anyContent.getValue()});
            classOf_AnyContent.getMethod("setType", String.class).invoke(_anyContent, new Object[] {anyContent.getType()});
            classOf_AnyContent.getMethod("setName", String.class).invoke(_anyContent, new Object[] {anyContent.getName()});
            classOf_AnyContent.getMethod("setEncoding", String.class).invoke(_anyContent, new Object[] {anyContent.getEncoding()});
            classOf_AnyContent.getMethod("setEmbeddingMethod", classOf_ValueEmbeddingEnumeration).invoke(_anyContent, new Object[] {getAdaptedValueEmbeddingEnumeration(anyContent.getEmbeddingMethod())});
            if (!anyContent.getItem().isEmpty()) {
                List _items = (List) classOf_AnyContent.getMethod("getItem").invoke(_anyContent);
                for (AnyContent item: anyContent.getItem()) {
                    _items.add(getAdaptedAnyContent(item));
                }
            }
        }
        return _anyContent;
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
        Object _validateRequest = classOf_ValidateRequest.getConstructor().newInstance();
        List inputs = (List)classOf_ValidateRequest.getMethod("getInput").invoke(_validateRequest);
        for (AnyContent input: validateRequest.getInput()) {
            inputs.add(getAdaptedAnyContent(input));
        }
        return _validateRequest;
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
            Method validateMethod = internalValidator.getClass().getMethod("validate", classOf_ValidateRequest);
            Object internalResult = validateMethod.invoke(internalValidator, new Object[] {getAdaptedValidateRequest(validateRequest)});
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
            Object _report = internalResult.getClass().getMethod("getReport").invoke(internalResult);
            Object _result = _report.getClass().getMethod("getResult").invoke(_report);
            // Value
            report.setResult(TestResultType.valueOf((String)_result.getClass().getMethod("value").invoke(_result)));
            // Counters
            Object _counters = _report.getClass().getMethod("getCounters").invoke(_report);
            report.setCounters(new ValidationCounters());
            if (_counters != null) {
                report.getCounters().setNrOfErrors((BigInteger)_counters.getClass().getMethod("getNrOfErrors").invoke(_counters));
                report.getCounters().setNrOfWarnings((BigInteger)_counters.getClass().getMethod("getNrOfWarnings").invoke(_counters));
                report.getCounters().setNrOfAssertions((BigInteger)_counters.getClass().getMethod("getNrOfAssertions").invoke(_counters));
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
            Object _reports = _report.getClass().getMethod("getReports").invoke(_report);
            if (_reports != null) {
                List _items = (List) _reports.getClass().getMethod("getInfoOrWarningOrError").invoke(_reports);
                if (_items != null) {
                    for (Object _item: _items) {
                        QName _itemName = (QName) _item.getClass().getMethod("getName").invoke(_item);
                        Object _itemBAR = _item.getClass().getMethod("getValue").invoke(_item);
                        if (_itemName != null && _itemBAR != null && _itemBAR.getClass().getName().equals("com.gitb.tr.BAR")) {
                            BAR bar = new BAR();
                            bar.setDescription((String) _itemBAR.getClass().getMethod("getDescription").invoke(_itemBAR));
                            bar.setType((String) _itemBAR.getClass().getMethod("getType").invoke(_itemBAR));
                            bar.setValue((String) _itemBAR.getClass().getMethod("getValue").invoke(_itemBAR));
                            bar.setLocation((String) _itemBAR.getClass().getMethod("getLocation").invoke(_itemBAR));
                            bar.setTest((String) _itemBAR.getClass().getMethod("getTest").invoke(_itemBAR));
                            bar.setAssertionID((String) _itemBAR.getClass().getMethod("getAssertionID").invoke(_itemBAR));
                            if (_itemName.getLocalPart().equals("error")) {
                                report.getReports().getInfoOrWarningOrError().add(TR_OBJECT_FACTORY.createTestAssertionGroupReportsTypeError(bar));
                            } else if (_itemName.getLocalPart().equals("warning")) {
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
