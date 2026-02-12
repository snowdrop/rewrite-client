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

    public LoggingService() {
    }

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

    public void setSpec(CommandLine.Model.CommandSpec spec) {
        this.spec = spec;
    }

    public void info(String message) {
        info(null, message);
    }

    public void info(Class<?> clazz, String message) {
        if (isCliMode) {
            spec.commandLine().getOut().println(colorizeMessage(LEVEL.INFO, clazz, message));
        } else {
            logger.info(message);
        }
    }

    public void warn(String message) {
        warn(null, message);
    }

    public void warn(Class<?> clazz, String message) {
        if (isCliMode) {
            spec.commandLine().getOut().println(colorizeMessage(LEVEL.WARN, clazz, message));
        } else {
            logger.warn(message);
        }
    }

    public void debug(String message) {
        debug(null, message);
    }

    public void debug(Class<?> clazz, String message) {
        if (isCliMode) {
            spec.commandLine().getOut().println(colorizeMessage(LEVEL.DEBUG, clazz, message));
        } else {
            logger.debug(message);
        }
    }

    public void trace(String message) {
        trace(null, message);
    }

    public void trace(Class<?> clazz, String message) {
        if (isCliMode) {
            spec.commandLine().getOut().println(colorizeMessage(LEVEL.TRACE, clazz, message));
        } else {
            logger.trace(message);
        }
    }

    public void fatal(String message) {
        fatal(null, message);
    }

    public void fatal(Class<?> clazz, String message) {
        if (isCliMode) {
            spec.commandLine().getOut().println(colorizeMessage(LEVEL.FATAL, clazz, message));
        } else {
            logger.fatal(message);
        }
    }

    public void error(String s) {
        this.error(null, s, null);
    }

    public void error(String message, Throwable e) {
        this.error(null, message, e);
    }

    public void error(Class<?> clazz, String message) {
        this.error(clazz, message, null);
    }

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

    public String colorizeMessage(LEVEL level, Class<?> clazz, String message) {
        return colorizeMessage(level, clazz, message, null);
    }

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

    private String loggerName(Class<?> clazz) {
        return clazz != null ? clazz.getName() : LoggingService.class.getName();
    }
}
