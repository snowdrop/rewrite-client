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
        if (isCliMode) {
            var formatedMessage = colorizeMessage(LEVEL.INFO, message);
            spec.commandLine().getOut().println(formatedMessage);
        } else {
            logger.info(message);
        }
    }

    public void warn(String message) {
        if (isCliMode) {
            spec.commandLine().getOut().println(colorizeMessage(LEVEL.WARN, message));
        } else {
            logger.warn(message);
        }
    }

    public void debug(String message) {
        if (isCliMode) {
            spec.commandLine().getOut().println(colorizeMessage(LEVEL.DEBUG, message));
        } else {
            logger.debug(message);
        }
    }

    public void trace(String message) {
        if (isCliMode) {
            spec.commandLine().getOut().println(colorizeMessage(LEVEL.TRACE, message));
        } else {
            logger.trace(message);
        }
    }

    public void fatal(String message) {
        if (isCliMode) {
            spec.commandLine().getOut().println(colorizeMessage(LEVEL.FATAL, message));
        } else {
            logger.fatal(message);
        }
    }

    public void error(String s) {
        this.error(s, null);
    }

    public void error(String message, Throwable e) {
        if (isCliMode) {
            if (e != null && isVerbose) {
                spec.commandLine().getErr().println(colorizeMessage(LEVEL.ERROR, message, e));
            } else {
                spec.commandLine().getOut().println(colorizeMessage(LEVEL.ERROR, message));
            }
        } else {
            if (isVerbose && e != null) {
                logger.error(message, e);
            } else {
                logger.error(message);
            }
        }
    }

    public String colorizeMessage(LEVEL level, String message) {
        return colorizeMessage(level, message, null);
    }

    public String colorizeMessage(LEVEL level, String message, Throwable ex) {
        ColorPatternFormatter fmt = new ColorPatternFormatter(darken, "%d{HH:mm:ss} %-5p %s%e");
        ExtLogRecord record = new ExtLogRecord(level.toJbossLevel(), message, ExtLogRecord.FormatStyle.PRINTF, LoggingService.class.getName());
        record.setParameters(new Object[]{Class.class}); // Set a dummy value otherwise the message is not rendered using the proper color !
        if(ex != null) {
            record.setThrown(ex);
        }
        return fmt.format(record);
    }
}
