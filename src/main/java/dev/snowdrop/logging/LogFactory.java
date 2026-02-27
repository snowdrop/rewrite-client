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
     *
     * Sets the verbose mode to log more traces, exceptions
     *
     * @param verbose the Verbose boolean
     */
    public void setVerboseMode(boolean verbose) {
        loggingService.setVerbose(verbose);
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