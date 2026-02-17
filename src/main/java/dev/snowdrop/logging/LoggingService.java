package dev.snowdrop.logging;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.aesh.terminal.tty.TerminalColorDetector;
import org.aesh.terminal.tty.TerminalConnection;
import org.aesh.terminal.utils.TerminalColorCapability;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.formatters.ColorPatternFormatter;
import picocli.CommandLine;

import java.io.IOException;

/**
 * Logging service that supports colored terminal output via Picocli
 * and falls back to JBoss logging when not running in CLI mode.
 */
@ApplicationScoped
public class LoggingService {
    private static final Logger logger = Logger.getLogger(LoggingService.class);

    @ConfigProperty(name = "client.mode", defaultValue = "false")
    boolean isCliMode;

    @ConfigProperty(name = "client.logging.verbose", defaultValue = "false")
    boolean isVerbose;

    private CommandLine.Model.CommandSpec spec;
    private TerminalColorCapability cap;
    private final static String SPACE = " ";
    private int darken;

    /**
     * Creates a new LoggingService instance.
     */
    public LoggingService() {
    }

    /**
     * Detects the terminal color capability and configures the darken setting.
     */
    @PostConstruct
    public void colorDetector() {
        try {
            TerminalConnection connection = new TerminalConnection();
            connection.openNonBlocking();
            cap = TerminalColorDetector.detect(connection);

            if (cap.getTheme().isDark()) {
                darken = 0;
            } else {
                darken = 1;
            }
            logger.debugf("Is the terminal dark: %s",cap.getTheme().isDark());
            connection.close();

            logger.debugf("Client mode enabled: %s",isCliMode);

        } catch (IOException e) {
            System.err.println("Error creating terminal connection: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Sets the Picocli command spec used for terminal output.
     *
     * @param spec the Picocli command spec
     */
    public void setSpec(CommandLine.Model.CommandSpec spec) {
        this.spec = spec;
    }

    /**
     * Logs a message at INFO level.
     *
     * @param message the message to log
     */
    public void info(String message) {
        info(null, message);
    }

    /**
     * Logs a message at INFO level with the originating class.
     *
     * @param clazz the originating class, or null
     * @param message the message to log
     */
    public void info(Class<?> clazz, String message) {
        if (isCliMode) {
            spec.commandLine().getOut().println(colorizeMessage(LEVEL.INFO, clazz, message));
        } else {
            logger.info(message);
        }
    }

    /**
     * Logs a message at WARN level.
     *
     * @param message the message to log
     */
    public void warn(String message) {
        warn(null, message);
    }

    /**
     * Logs a message at WARN level with the originating class.
     *
     * @param clazz the originating class, or null
     * @param message the message to log
     */
    public void warn(Class<?> clazz, String message) {
        if (isCliMode) {
            spec.commandLine().getOut().println(colorizeMessage(LEVEL.WARN, clazz, message));
        } else {
            logger.warn(message);
        }
    }

    /**
     * Logs a message at DEBUG level.
     *
     * @param message the message to log
     */
    public void debug(String message) {
        debug(null, message);
    }

    /**
     * Logs a message at DEBUG level with the originating class.
     *
     * @param clazz the originating class, or null
     * @param message the message to log
     */
    public void debug(Class<?> clazz, String message) {
        if (isCliMode) {
            spec.commandLine().getOut().println(colorizeMessage(LEVEL.DEBUG, clazz, message));
        } else {
            logger.debug(message);
        }
    }

    /**
     * Logs a message at TRACE level.
     *
     * @param message the message to log
     */
    public void trace(String message) {
        trace(null, message);
    }

    /**
     * Logs a message at TRACE level with the originating class.
     *
     * @param clazz the originating class, or null
     * @param message the message to log
     */
    public void trace(Class<?> clazz, String message) {
        if (isCliMode) {
            spec.commandLine().getOut().println(colorizeMessage(LEVEL.TRACE, clazz, message));
        } else {
            logger.trace(message);
        }
    }

    /**
     * Logs a message at FATAL level.
     *
     * @param message the message to log
     */
    public void fatal(String message) {
        fatal(null, message);
    }

    /**
     * Logs a message at FATAL level with the originating class.
     *
     * @param clazz the originating class, or null
     * @param message the message to log
     */
    public void fatal(Class<?> clazz, String message) {
        if (isCliMode) {
            spec.commandLine().getOut().println(colorizeMessage(LEVEL.FATAL, clazz, message));
        } else {
            logger.fatal(message);
        }
    }

    /**
     * Logs a message at ERROR level.
     *
     * @param s the message to log
     */
    public void error(String s) {
        this.error(null, s, null);
    }

    /**
     * Logs a message at ERROR level with an exception.
     *
     * @param message the message to log
     * @param e the exception to log
     */
    public void error(String message, Throwable e) {
        this.error(null, message, e);
    }

    /**
     * Logs a message at ERROR level with the originating class.
     *
     * @param clazz the originating class, or null
     * @param message the message to log
     */
    public void error(Class<?> clazz, String message) {
        this.error(clazz, message, null);
    }

    /**
     * Logs a message at ERROR level with the originating class and an exception.
     *
     * @param clazz the originating class, or null
     * @param message the message to log
     * @param e the exception to log, or null
     */
    public void error(Class<?> clazz, String message, Throwable e) {
        if (isCliMode) {
            if (e != null && isVerbose) {
                spec.commandLine().getErr().println(colorizeMessage(LEVEL.ERROR, clazz, message, e));
            } else {
                spec.commandLine().getOut().println(colorizeMessage(LEVEL.ERROR, clazz, message));
            }
        } else {
            if (isVerbose && e != null) {
                logger.error(message, e);
            } else {
                logger.error(message);
            }
        }
    }

    /**
     * Colorizes a log message using the JBoss ColorPatternFormatter.
     *
     * @param level the log level
     * @param clazz the originating class, or null
     * @param message the message text
     * @return the colorized message string
     */
    public String colorizeMessage(LEVEL level, Class<?> clazz, String message) {
        return colorizeMessage(level, clazz, message, null);
    }

    /**
     * Colorizes a log message with an optional exception using the JBoss ColorPatternFormatter.
     *
     * @param level the log level
     * @param clazz the originating class, or null
     * @param message the message text
     * @param ex the exception to include, or null
     * @return the colorized message string
     */
    public String colorizeMessage(LEVEL level, Class<?> clazz, String message, Throwable ex) {
        String name = loggerName(clazz);
        ColorPatternFormatter fmt = new ColorPatternFormatter(darken, "%d{HH:mm:ss} %-5p [%c] %s%e");
        ExtLogRecord record = new ExtLogRecord(level.toJbossLevel(), message, ExtLogRecord.FormatStyle.PRINTF, name);
        record.setLoggerName(name);
        record.setParameters(new Object[]{clazz.getName()});
        if (ex != null) {
            record.setThrown(ex);
        }
        return fmt.format(record);
    }

    /**
     * Returns the logger name for the given class.
     *
     * @param clazz the class, or null to use LoggingService
     * @return the fully qualified class name
     */
    private String loggerName(Class<?> clazz) {
        return clazz != null ? clazz.getName() : LoggingService.class.getName();
    }
}