package dev.snowdrop.rewrite.cli;

import dev.snowdrop.rewrite.config.RewriteConfig;
import dev.snowdrop.rewrite.toolbox.DataTableUtils;
import org.junit.jupiter.api.BeforeEach;
import org.openrewrite.RecipeRun;

import java.util.List;
import java.util.Set;

public class BaseTest {

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
