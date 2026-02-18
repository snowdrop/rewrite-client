package dev.snowdrop.openrewrite.cli.toolbox;

import org.openrewrite.DataTable;
import org.openrewrite.RecipeRun;

import java.util.List;
import java.util.Map;

public class DataTableUtils {

    private DataTableUtils() {
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> findDataTableRows(RecipeRun run, String dataTableName, Class<T> rowType) {
        if (run.getDataTables().isEmpty()) {
            throw new IllegalArgumentException("DataTables should not be empty");
        }
        return run.getDataTables().entrySet().stream()
            .filter(entry -> entry.getKey().getName().contains(dataTableName))
            .findFirst()
            .map(entry -> (List<T>) entry.getValue())
            .orElseThrow(() -> new IllegalArgumentException("DataTable containing '" + dataTableName + "' not found"));
    }
}