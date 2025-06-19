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

package eu.europa.ec.itb.validation.commons.jar;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Filter used to disable console or file logging (or both).
 */
public class LoggingFilter extends Filter<ILoggingEvent> {

    /**
     * Decide whether to log based on the previously recorded command line arguments.
     *
     * @param event The logging event.
     * @return Whether logging should proceed.
     */
    @Override
    public FilterReply decide(ILoggingEvent event) {
        FilterReply decision = FilterReply.NEUTRAL;
        var forConsole = BaseValidationRunner.LOGGER_FEEDBACK.getName().equals(event.getLoggerName());
        if ((forConsole && !CommandLineValidator.isConsoleOutputOn()) || (!forConsole && !CommandLineValidator.isFileOutputOn())) {
            decision = FilterReply.DENY;
        }
        return decision;
    }

}
