package dev.snowdrop.rewrite.cli.logging;

import jakarta.inject.Singleton;
import org.aesh.terminal.tty.TerminalColorDetector;
import org.aesh.terminal.tty.TerminalConnection;
import org.jboss.logging.Logger;
import org.jboss.logmanager.LogManager;
import org.jboss.logmanager.formatters.ColorPatternFormatter;
import picocli.CommandLine;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

/**
 * Utility class able create a ColorHandler, register it for each logger - category
 * During the creation process, aesh is used to detect if the terminal is dark or light
 */
@Singleton
public class LoggerUtils {
    private static final Logger LOG = Logger.getLogger(LoggerUtils.class);
    private final static LogManager logManager = (LogManager) LogManager.getLogManager();

    /**
     * Register the ColorHandler to the different loggers using the JBoss LogManager
     *
     * @param cfg The logging configuration with parameters coming from application.properties or picocli command
     * @param verbosity The verbosity level selected by the user: -v or -vv
     * @param spec The Picocli CommandSpec able to provide the Print writers: out or err
     */
    public void setupLogManagerAndHandler(LoggingConfiguration cfg, int verbosity, CommandLine.Model.CommandSpec spec) {
        ColorHandler colorHandler = new ColorHandler(spec);
        colorHandler.setFormatter(new ColorPatternFormatter(isTerminalDark(), cfg.format()));

        // Clean up handlers on the root logger
        final var rootLogger = logManager.getLogger("");
        for (var handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        // Set the root level (= no category or package name defined !)
        rootLogger.setLevel(Level.parse(cfg.level()));

        // Iterate over the list of the categories (= package names) to register their level
        for (Map.Entry<String, LoggingConfiguration.LevelConfig> category : cfg.levels().entrySet()) {
            final var logger = logManager.getLogger(category.getKey());
            logger.addHandler(colorHandler);
            if (verbosity == 0) {
                logger.setLevel(Level.parse(category.getValue().level()));
            } else {
                // System.out.printf("Verbosity: %d, strLevel: %s and Level: %s.%n",verbosity,getLevelFromVerbosity(verbosity),java.util.logging.Level.parse(getLevelFromVerbosity(verbosity)));
                logger.setLevel(Level.parse(getLevelFromVerbosity(verbosity)));
            }
        }
    }

    /**
     * Convert the verbosity value to its corresponding Log Level
     *
     * @param verbosity The verbosity level selected by the user: -v or -vv
     * @return The string Level
     */
    private static String getLevelFromVerbosity(int verbosity) {
        return switch (verbosity) {
            case 1  -> "DEBUG";  // -v
            case 2  -> "TRACE"; // -vv
            default -> "INFO";
        };
    }

    /**
     * Detect if the terminal is dark or light
     *
     * @return The int value 0 or 1 indicating if the terminal is light or dark
     */
    private static int isTerminalDark() {
        int darken = 0;
        try {
            long start = System.currentTimeMillis();
            TerminalConnection connection = new TerminalConnection();
            var cap = TerminalColorDetector.detect(connection);
            long elapsed = System.currentTimeMillis() - start;
            LOG.debugf("Theme detection took: [%s ms]%n", elapsed);

            darken = cap.getTheme().isDark() ? 0 : 1;
        } catch (IOException ex) {
            LOG.error("Failed to detect the terminal color !",ex);
        }
        return darken;
    }
}
