package dev.snowdrop.rewrite.logging;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.formatters.ColorPatternFormatter;

import java.io.PrintWriter;

/**
 * Color log handler
 */
public class ColorHandler extends ExtHandler {

    private final PrintWriter out;
    private final PrintWriter err;

    /**
     * Creates a new handler with the given command spec and darken level.
     *
     */
    public ColorHandler() {
        out = new PrintWriter(System.out);
        err = new PrintWriter(System.err);
    }

    @Override
    protected void doPublish(ExtLogRecord record) {

        int recordLevel = record.getLevel().intValue();
        int loggerLevel = org.jboss.logmanager.Level.ERROR.intValue();

        PrintWriter writer = recordLevel >= loggerLevel ? err : out;

        writer.print(super.getFormatter().format(record));
        writer.flush();
    }
}