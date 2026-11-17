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

package eu.europa.ec.itb.validation.commons.error;

import eu.europa.ec.itb.validation.commons.LocalisationHelper;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Exception class used to denote exceptions the messages of which should be directly presented to users.
 */
public class ValidatorException extends RuntimeException {

    /**
     * The default message.
     */
    public static final String MESSAGE_DEFAULT = "validator.label.exception.default";
    /**
     * The message template used to prefix a message with the input that caused it.
     */
    public static final String MESSAGE_FOR_INPUT = "validator.label.exception.forInput";

    private final transient Object[] messageParams;
    private final boolean localised;
    private final String originalMessage;
    private String inputReference;

    /**
     * Run the provided action, making sure that any failure is linked to the provided request input.
     * <p>
     * A ValidatorException raised by the action is rethrown with the input reference attached (unless it already
     * has one). Any other exception is wrapped as a ValidatorException using the provided message key, with the
     * cause's message as the key's parameter.
     *
     * @param inputReference The reference of the input (e.g. "contentToValidate[1]"). If null the action is simply run.
     * @param messageKey The message key to use for wrapping non-ValidatorExceptions.
     * @param action The action to run.
     * @param <T> The type of result.
     * @return The action's result.
     */
    public static <T> T forInput(String inputReference, String messageKey, Supplier<T> action) {
        try {
            return action.get();
        } catch (ValidatorException e) {
            throw e.withInputReference(inputReference);
        } catch (Exception e) {
            throw new ValidatorException(messageKey, e, e.getMessage()).withInputReference(inputReference);
        }
    }

    /**
     * Variant of {@link #forInput(String, String, Supplier)} for actions without a result.
     *
     * @param inputReference The reference of the input (e.g. "contentToValidate[1]").
     * @param messageKey The message key to use for wrapping non-ValidatorExceptions.
     * @param action The action to run.
     */
    public static void forInput(String inputReference, String messageKey, Runnable action) {
        forInput(inputReference, messageKey, () -> {
            action.run();
            return null;
        });
    }

    /**
     * Link this exception to a request input. If the exception is already linked to an input (the innermost, most
     * specific reference) it is not replaced.
     *
     * @param inputReference The input reference (e.g. "externalRules[0]"). Ignored if null.
     * @return This exception.
     */
    public ValidatorException withInputReference(String inputReference) {
        if (this.inputReference == null) {
            this.inputReference = inputReference;
        }
        return this;
    }

    /**
     * Prefix the input reference with the provided one. This is used when the exception originates from a nested input,
     * for example from one of the entries of a request that provides multiple inputs to validate ("[2].externalRules[0]").
     * If the exception has no input reference, the prefix becomes the reference.
     *
     * @param prefix The prefix (e.g. "[2]").
     * @return This exception.
     */
    public ValidatorException withInputReferencePrefix(String prefix) {
        if (prefix != null) {
            this.inputReference = (this.inputReference == null) ? prefix : prefix + "." + this.inputReference;
        }
        return this;
    }

    /**
     * @return The reference of the request input that caused this exception (e.g. "contentToValidate[1]"), or null if unknown.
     */
    public String getInputReference() {
        return inputReference;
    }

    /**
     * @param cause The root cause.
     */
    public ValidatorException(Throwable cause) {
        this(MESSAGE_DEFAULT, cause);
    }

    /**
     * @param message The message to display.
     */
	public ValidatorException(String message) {
		this(message, null, (Object[]) null);
	}

    /**
     * @param message The message to display.
     * @param localised Whether the message is already localised.
     */
    public ValidatorException(String message, boolean localised) {
        this(message, null, localised, (Object[]) null);
    }

    /**
     * @param message The message to display.
     * @param messageParams The parameters for the message.
     */
    public ValidatorException(String message, Object... messageParams) {
        this(message, null, messageParams);
    }

    /**
     * @param message The message to display.
     * @param cause The root cause.
     * @param messageParams The parameters for the message.
     */
    public ValidatorException(String message, Throwable cause, Object... messageParams) {
        this(message, cause, false, messageParams);
    }

    /**
     * @param message The message to display.
     * @param cause The root cause.
     * @param localised Whether the message is already localised.
     * @param messageParams The parameters for the message.
     */
    public ValidatorException(String message, Throwable cause, boolean localised, Object... messageParams) {
        super(message, cause);
        this.originalMessage = message;
        this.messageParams = messageParams;
        this.localised = localised;
    }

    /**
     * @return The parameters for the message's customisation.
     */
    public Object[] getMessageParams() {
        return messageParams;
    }

    /**
     * @return Whether the message is already localised.
     */
    public boolean isLocalised() {
        return localised;
    }

    /**
     * @return The message to include in log files.
     */
    public String getMessageForLog() {
        return getLocalisedMessage(new LocalisationHelper(Locale.ENGLISH));
    }

    /**
     * @param helper The localisation helper to use.
     * @return The message to display to users.
     */
    public String getMessageForDisplay(LocalisationHelper helper) {
        return getLocalisedMessage(helper);
    }

    /**
     * Localise the message based on the provided localisation helper.
     *
     * @param helper The helper to use.
     * @return The localised message.
     */
    private String getLocalisedMessage(LocalisationHelper helper) {
        if (isLocalised()) {
            return originalMessage;
        }
        String message = helper.localise(originalMessage, getMessageParams());
        if (inputReference != null) {
            message = helper.localise(MESSAGE_FOR_INPUT, inputReference, message);
        }
        return message;
    }

    /**
     * Make sure the stacktrace returns the localised message when printed in logs.
     *
     * @return The localised message.
     */
    @Override
    public String getMessage() {
        return getMessageForLog();
    }

}
