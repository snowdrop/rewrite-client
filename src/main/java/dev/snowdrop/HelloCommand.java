///usr/bin/env jbang “$0” “$@” ; exit $?
//DEPS dev.snowdrop.openrewrite:rewrite-client:0.2.7-SNAPSHOT
//SOURCES logging/LoggingService.java
//SOURCES logging/PicocliColorHandler.java
//DEPS io.quarkus.platform:quarkus-bom:3.29.4@pom
//DEPS org.jboss.logmanager:jboss-logmanager:3.2.1.Final
//DEPS io.quarkus:quarkus-picocli
package dev.snowdrop;

import dev.snowdrop.logging.PicocliColorHandler;
import org.jboss.logmanager.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import org.jboss.logmanager.Level;

@Command(name = "hello", mixinStandardHelpOptions = true, version = "1.0",
        description = "Enhanced Picocli + JBoss LogManager Integration")
public class HelloCommand implements Runnable {

    @Spec
    CommandSpec spec;

    Logger rootLogger;

    @Override
    public void run() {
        setupPicocliHandler();
        Logger log = Logger.getLogger("HelloCommand");

        log.log(Level.TRACE, "This is a TRACE message.");
        log.log(Level.DEBUG, "This is a DEBUG message.");
        log.log(Level.INFO, "This is a INFO message.");
        log.log(Level.INFO, "Hello! Logging is now routed through Picocli Handlers.");
        log.log(Level.WARN, "This is a WARNING message in yellow/gold.");
    }

    private void setupPicocliHandler() {
        rootLogger = Logger.getLogger("");

        int darken = detectDarkenLevel();
        PicocliColorHandler handler = new PicocliColorHandler(spec, darken);
        handler.setLevel(Level.TRACE);
        rootLogger.addHandler(handler);
        rootLogger.setLevel(Level.ALL);
    }

    private int detectDarkenLevel() {
        // COLORFGBG environment variable is common in many terminals (value like 15;0)
        // A background of '0' usually means dark.
        String env = System.getenv("COLORFGBG");
        if (env != null && env.contains(";")) {
            String bg = env.substring(env.lastIndexOf(";") + 1);
            try {
                // If the background index is low (like 0), it's dark.
                return Integer.parseInt(bg) < 8 ? 0 : 1;
            } catch (NumberFormatException e) { return 0; }
        }
        return 0; // Default to Dark mode (most common for devs)
    }

    public static void main(String[] args) {
        // Bootstrapping JBoss LogManager if not set via -Djava.util.logging.manager
        if (System.getProperty("java.util.logging.manager") == null) {
            System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        }

        int exitCode = new CommandLine(new HelloCommand()).execute(args);
        System.exit(exitCode);
    }
}