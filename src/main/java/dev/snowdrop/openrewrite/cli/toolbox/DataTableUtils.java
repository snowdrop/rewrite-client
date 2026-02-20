package dev.snowdrop.openrewrite.cli.toolbox;

import org.openrewrite.RecipeRun;

import java.util.List;

/**
 * Utility class to handle the Data of a Table
 */
public class DataTableUtils {

    private DataTableUtils() {
    }

    /**
     * Find a DataTable object from the list of the DataTables created from recipes executed
     *
     * @param run the RecipeRun executed
     * @param dataTableName the name of the DataTable to search about (example: SearchResults)
     * @param rowType The OpenRewrite Row record class
     * @return The list of the Rows
     * @param <T> The type of the Row record
     */
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