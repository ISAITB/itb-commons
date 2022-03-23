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
