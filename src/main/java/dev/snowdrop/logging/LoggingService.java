package dev.snowdrop.logging;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.aesh.terminal.tty.TerminalColorDetector;
import org.aesh.terminal.tty.TerminalConnection;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import picocli.CommandLine;

import java.io.IOException;

import static org.jboss.logmanager.Level.*;

/**
 * Alternative logging service that routes log output through JBoss LogManager handlers.
 */
@ApplicationScoped
public class LoggingService {

    /** Creates a new LoggingService instance. */
    public LoggingService() {
    }

    private static final org.jboss.logmanager.Logger rootLogger =
            org.jboss.logmanager.Logger.getLogger("");

    @ConfigProperty(name = "client.mode", defaultValue = "false")
    boolean isCliMode;

    private CommandLine.Model.CommandSpec spec;
    private int darken = 0;
    private PicocliColorHandler activeHandler;

    /**
     * Initializes terminal color detection when running in CLI mode.
     */
    @PostConstruct
    public void init() {
        if (isCliMode) {
            detectTerminalColor();
        }
    }

    private void detectTerminalColor() {
        try (TerminalConnection connection = new TerminalConnection()) {
            connection.openNonBlocking();
            var cap = TerminalColorDetector.detect(connection);
            darken = cap.getTheme().isDark() ? 0 : 1;
        } catch (IOException e) {
            // Fallback to default darken if terminal detection fails
        }
    }

    /**
     * Sets the Picocli command spec and reconfigures JBoss logging in CLI mode.
     *
     * @param spec the Picocli command spec
     */
    public void setSpec(CommandLine.Model.CommandSpec spec) {
        this.spec = spec;
        if (isCliMode) {
            reconfigureJBossLogging();
        }
    }

    private void reconfigureJBossLogging() {
        if (activeHandler != null) {
            rootLogger.removeHandler(activeHandler);
        }

        activeHandler = new PicocliColorHandler(spec, darken);
        rootLogger.setLevel(ALL);
        rootLogger.addHandler(activeHandler);
    }

    /**
     * Logs a message at INFO level.
     *
     * @param message the message to log
     */
    public void info(String message) {
        rootLogger.info(message);
    }

    /**
     * Logs a message at DEBUG level.
     *
     * @param message the message to log
     */
    public void debug(String message) {
        rootLogger.log(DEBUG,message);
    }

    /**
     * Logs a message at TRACE level.
     *
     * @param message the message to log
     */
    public void trace(String message) {
        rootLogger.log(TRACE,message);
    }

    /**
     * Logs a message at WARN level.
     *
     * @param message the message to log
     */
    public void warn(String message) {
        rootLogger.log(WARN,message);
    }

    /**
     * Logs a message at FATAL level.
     *
     * @param message the message to log
     */
    public void fatal(String message) {
        rootLogger.log(FATAL,message);
    }

    /**
     * Logs a message at ERROR level with an exception.
     *
     * @param message the message to log
     * @param e the exception to log
     */
    public void error(String message, Throwable e) {
        rootLogger.log(org.jboss.logmanager.Level.ERROR, message, e);
    }
}