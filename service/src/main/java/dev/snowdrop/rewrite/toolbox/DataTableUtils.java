package dev.snowdrop.rewrite.toolbox;

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
     * @param run           the RecipeRun executed
     * @param dataTableName the name of the DataTable to search about (example: SearchResults)
     * @param rowType       The OpenRewrite Row record class
     * @param <T>           The type of the Row record
     * @return The list of the Rows
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> findDataTableRows(RecipeRun run, String dataTableName, Class<T> rowType) {
        if (run.getDataTableStore().getDataTables().isEmpty()) {
            throw new IllegalArgumentException("DataTables should not be empty");
        }
        return run.getDataTableStore().getDataTables().stream()
                .filter(entry -> entry.getType().getName().contains(dataTableName))
                .filter(entry -> entry.getType().equals(rowType))
                .findFirst()
                .map(dt -> {
                    @SuppressWarnings("rawtypes")
                    Class dtClass = dt.getClass();
                    return (List<T>) run.getDataTableStore().getRows(dtClass).toList();
                })
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("DataTable '%s' with row type %s not found", dataTableName, rowType.getSimpleName())
                ));
    }
}