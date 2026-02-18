package dev.snowdrop.rewrite.cli;

import dev.snowdrop.openrewrite.cli.RewriteCommand;
import dev.snowdrop.openrewrite.cli.model.RewriteConfig;
import dev.snowdrop.openrewrite.cli.toolbox.DataTableUtils;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.openrewrite.RecipeRun;

import java.util.List;
import java.util.Set;

public class BaseTest {

    @Inject
    @TopCommand
    public RewriteCommand rewriteCmd;

    public RewriteConfig cfg;

    @BeforeEach
    public void beforeEach() {
        cfg = new RewriteConfig();
        cfg.setExportDatatables(true);
        cfg.setExclusions(Set.of());
        cfg.setPlainTextMasks(Set.of());
        cfg.setAdditionalJarPaths(List.of());
        cfg.setDryRun(true);
    }

    protected <T> List<T> findDataTableRows(RecipeRun run, String dataTableName, Class<T> rowType) {
        return DataTableUtils.findDataTableRows(run, dataTableName, rowType);
    }
}
