package dev.snowdrop.rewrite.cli.logging;

import org.aesh.terminal.tty.TerminalColorDetector;
import org.aesh.terminal.tty.TerminalConnection;
import org.jboss.logging.Logger;
import org.jboss.logmanager.LogManager;
import org.jboss.logmanager.formatters.ColorPatternFormatter;
import picocli.CommandLine;

import java.io.IOException;
import java.util.Map;

public class LoggerUtils {
    private static final Logger LOG = Logger.getLogger("");
    private final static LogManager logManager = (LogManager) LogManager.getLogManager();

    public void setupLogManagerAndHandler(LoggingConfiguration cfg, int verbosity, CommandLine.Model.CommandSpec spec) {
        ColorHandler colorHandler = new ColorHandler(spec);
        colorHandler.setFormatter(new ColorPatternFormatter(isTerminalDark(), cfg.format()));

        // Clean up handlers on the root logger
        final var rootLogger = logManager.getLogger("");
        for (var handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        // Set the root level (= no category or package name defined !)
        // logManager.getLogger("").addHandler(colorHandler);
        logManager.getLogger("").setLevel(java.util.logging.Level.parse(cfg.level()));

        // Iterate over the list of the categories (= package names) to register their level
        for (Map.Entry<String, LoggingConfiguration.LevelConfig> category : cfg.levels().entrySet()) {
            logManager.getLogger(category.getKey()).addHandler(colorHandler);
            if (verbosity == 0) {
                logManager.getLogger(category.getKey()).setLevel(java.util.logging.Level.parse(category.getValue().level()));
            } else {
                System.out.printf("Verbosity: %d, strLevel: %s and Level: %s.%n",verbosity,getLevelFromVerbosity(verbosity),java.util.logging.Level.parse(getLevelFromVerbosity(verbosity)));
                logManager.getLogger(category.getKey()).setLevel(java.util.logging.Level.parse(getLevelFromVerbosity(verbosity)));
            }
        }
    }

    private String getLevelFromVerbosity(int verbosity) {
        return switch (verbosity) {
            case 1  -> "WARN";  // -v
            case 2  -> "DEBUG"; // -vv
            case 3  -> "TRACE"; // -vvv
            default -> "INFO";
        };
    }

    private int isTerminalDark() {
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
