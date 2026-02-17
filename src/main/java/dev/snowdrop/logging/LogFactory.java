package dev.snowdrop.logging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import picocli.CommandLine;

/**
 * Log factory for obtaining and configuring the {@link LoggingService}.
 */
@ApplicationScoped
public class LogFactory {

    /** Default constructor. */
    public LogFactory() {
    }

    @Inject
    LoggingService loggingService;

    /**
     * Sets the Picocli command spec used for terminal output.
     *
     * @param spec the Picocli command spec
     */
    public void setSpec(CommandLine.Model.CommandSpec spec) {
        loggingService.setSpec(spec);
    }

    /**
     * Returns the injected logging service.
     *
     * @return the logging service instance
     */
    public LoggingService getLogger() {
        return loggingService;
    }
}