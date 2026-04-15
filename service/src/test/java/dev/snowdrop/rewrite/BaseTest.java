package dev.snowdrop.rewrite;

import dev.snowdrop.rewrite.config.RewriteConfig;
import org.junit.jupiter.api.BeforeEach;
import org.openrewrite.DataTable;
import org.openrewrite.RecipeRun;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

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

    /**
     * Find the rows of a DataTable from the DataTableStore of the recipe executed
     *
     * @param run           the RecipeRun executed
     * @param dataTableType The OpenRewrite DataTable class
     * @param <Row>         The type of the Row record
     * @return The stream of the Row records
     */
    protected <Row> Stream<Row> findDataTableRows(RecipeRun run, Class<? extends DataTable<Row>> dataTableType) {
        if (run.getDataTableStore().getDataTables().isEmpty()) {
            throw new IllegalArgumentException("DataTables should not be empty");
        }
        return run.getDataTableStore().getRows(dataTableType);
    }
}
