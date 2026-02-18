package dev.snowdrop.logging;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.formatters.ColorPatternFormatter;
import picocli.CommandLine;

import java.io.PrintWriter;

/**
 * JBoss log handler that routes colored log output to Picocli stdout/stderr.
 */
public class PicocliColorHandler extends ExtHandler {
    private final CommandLine.Model.CommandSpec spec;
    private final ColorPatternFormatter formatter;

    /**
     * Creates a new handler with the given command spec and darken level.
     *
     * @param spec the Picocli command spec for output routing
     * @param darken the darken level for the color formatter
     */
    public PicocliColorHandler(CommandLine.Model.CommandSpec spec, int darken) {
        this.spec = spec;
        this.formatter = new ColorPatternFormatter(darken, "%d{HH:mm:ss} %-5p [%c{2.}] (%t) %s%e%n");
    }

    @Override
    protected void doPublish(ExtLogRecord record) {
        // Route ERROR/FATAL to stderr, others to stdout
        PrintWriter writer = (record.getLevel().intValue() >= org.jboss.logmanager.Level.ERROR.intValue())
                ? spec.commandLine().getErr()
                : spec.commandLine().getOut();

        writer.print(formatter.format(record));
        writer.flush();
    }
}