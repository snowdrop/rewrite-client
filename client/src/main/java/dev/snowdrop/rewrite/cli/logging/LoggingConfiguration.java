package dev.snowdrop.rewrite.cli.logging;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.Map;

@ConfigMapping(prefix = "cli.log")
public interface LoggingConfiguration {

    /**
     * @return The format to log the message
     */
    @WithDefault("%d{HH:mm:ss} %-5p [%c{2.}] (%t) %s%e%n")
    String format();

    /**
     * @return The logger level of the Handler
     */
    @WithDefault("INFO")
    String level();

    @WithName("category")
    Map<String, LevelConfig> levels();

    interface LevelConfig {
        @WithDefault("INFO")
        String level();
    }
}