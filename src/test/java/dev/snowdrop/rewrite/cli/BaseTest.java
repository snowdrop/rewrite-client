package dev.snowdrop.rewrite.cli;

import dev.snowdrop.openrewrite.cli.model.RewriteConfig;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Set;

public class BaseTest {

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
