package dev.snowdrop.rewrite.cli;

import picocli.CommandLine;
import org.eclipse.microprofile.config.ConfigProvider;

public class QuarkusVersionProvider implements CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() {
        String version = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.application.version", String.class)
                .orElse("0.0.1-SNAPSHOT");
        return new String[] { version };
    }
}
