package dev.snowdrop.rewrite.cli.toolbox;

import org.aesh.terminal.tty.TerminalColorDetector;
import org.aesh.terminal.tty.TerminalConnection;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.LogManager;
import org.jboss.logmanager.formatters.ColorPatternFormatter;
import picocli.CommandLine;

import java.io.IOException;

public class LoggerUtils {
    static LogManager logManager = (LogManager) LogManager.getLogManager();

    public static void setupLogManagerAndHandler(String logMsgFormat, String logMsgLevel, CommandLine.Model.CommandSpec spec, int darken) {
        ColorHandler handler = new ColorHandler(spec);
        handler.setLevel(Level.ALL);
        handler.setFormatter(new ColorPatternFormatter(darken, logMsgFormat));

        logManager.getLogger("dev.snowdrop.rewrite").addHandler(handler);
        logManager.getLogger("dev.snowdrop.rewrite").setLevel(java.util.logging.Level.parse(logMsgLevel));
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
