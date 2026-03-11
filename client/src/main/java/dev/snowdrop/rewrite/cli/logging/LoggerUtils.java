package dev.snowdrop.rewrite.cli.logging;

import org.aesh.terminal.tty.TerminalColorDetector;
import org.aesh.terminal.tty.TerminalConnection;
import org.jboss.logmanager.LogManager;
import org.jboss.logmanager.formatters.ColorPatternFormatter;
import picocli.CommandLine;

import java.io.IOException;
import java.util.Map;

public class LoggerUtils {
    static LogManager logManager = (LogManager) LogManager.getLogManager();

    public static void setupLogManagerAndHandler(LoggingConfiguration cfg, CommandLine.Model.CommandSpec spec, int darken) {
        ColorHandler handler = new ColorHandler(spec);
        handler.setLevel(java.util.logging.Level.parse(cfg.level()));
        handler.setFormatter(new ColorPatternFormatter(darken, cfg.format()));

        for (Map.Entry<String, LoggingConfiguration.LevelConfig> category : cfg.levels().entrySet()) {
            logManager.getLogger(category.getKey()).addHandler(handler);
            logManager.getLogger(category.getKey()).setLevel(java.util.logging.Level.parse(category.getValue().level()));
        }
    }

    public static int isTerminalDark() {
        int darken = 0;
        try {
            long start = System.currentTimeMillis();
            TerminalConnection connection = new TerminalConnection();
            var cap = TerminalColorDetector.detect(connection);
            long elapsed = System.currentTimeMillis() - start;
            System.out.printf("Theme detection took: [%s ms]%n", elapsed);

            darken = cap.getTheme().isDark() ? 0 : 1;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return darken;
    }
}
