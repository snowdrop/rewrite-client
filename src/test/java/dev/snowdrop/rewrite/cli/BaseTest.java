package dev.snowdrop.rewrite.cli;

import dev.snowdrop.openrewrite.cli.RewriteCommand;
import dev.snowdrop.openrewrite.cli.model.RewriteConfig;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Set;

public class BaseTest {

    @Inject
    @TopCommand
    RewriteCommand rewriteCmd;

    RewriteConfig cfg;

    @BeforeEach
    public void beforeEach() {
        cfg = new RewriteConfig();
        cfg.setExportDatatables(true);
        cfg.setExclusions(Set.of());
        cfg.setPlainTextMasks(Set.of());
        cfg.setAdditionalJarPaths(List.of());
        cfg.setDryRun(true);
    }
}
