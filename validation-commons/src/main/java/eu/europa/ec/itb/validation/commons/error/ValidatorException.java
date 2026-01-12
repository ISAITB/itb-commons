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

/**
 * Exception class used to denote exceptions the messages of which should be directly presented to users.
 */
public class ValidatorException extends RuntimeException {

    /**
     * The default message.
     */
    public static final String MESSAGE_DEFAULT = "validator.label.exception.default";

    private final transient Object[] messageParams;
    private final boolean localised;
    private final String originalMessage;

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
        return isLocalised()?originalMessage:helper.localise(originalMessage, getMessageParams());
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
