package dev.snowdrop.logging;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import picocli.CommandLine;

@ApplicationScoped
public class LogFactory {

    @Inject
    LoggingService loggingService;

    public void setSpec(CommandLine.Model.CommandSpec spec) {
        loggingService.setSpec(spec);
    }

    public LoggingService getLogger() {
        return loggingService;
    }
}
