package dev.snowdrop.rewrite.cli.toolbox;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import picocli.CommandLine;

import java.io.PrintWriter;

/**
 * JBoss log handler that routes colored log output to Picocli stdout/stderr.
 */
public class ColorHandler extends ExtHandler {
    private final CommandLine.Model.CommandSpec spec;

    /**
     * Creates a new handler with the given command spec and darken level.
     *
     * @param spec the Picocli command spec for output routing
     */
    public ColorHandler(CommandLine.Model.CommandSpec spec) {
        this.spec = spec;
    }

    @Override
    protected void doPublish(ExtLogRecord record) {
        // Route ERROR/FATAL to stderr, others to stdout
        PrintWriter writer = (record.getLevel().intValue() >= org.jboss.logmanager.Level.ERROR.intValue())
                ? spec.commandLine().getErr()
                : spec.commandLine().getOut();

        writer.print(super.getFormatter().format(record));
        writer.flush();
    }
}