package dev.snowdrop;

import dev.snowdrop.openrewrite.cli.RewriteScanner;
import dev.snowdrop.openrewrite.cli.model.Config;
import dev.snowdrop.openrewrite.cli.model.ResultsContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openrewrite.DataTable;
import org.openrewrite.RecipeRun;
import org.openrewrite.table.SourcesFileResults;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AutoFormatJavaAppTest {
    @Test
    public void shouldAutoFormatTest() {
        String RECIPE_NAME = "org.openrewrite.java.format.AutoFormat";
        String APP_PATH = "demo";

        Config cfg = new Config();
        cfg.setAppPath(Paths.get(APP_PATH));
        cfg.setActiveRecipes(List.of(RECIPE_NAME));

        RewriteScanner scanner = new RewriteScanner(cfg);
        scanner.init();
        try {
            ResultsContainer results = scanner.run();
            RecipeRun run = results.getRecipeRuns().get(RECIPE_NAME);

            Optional<Map.Entry<DataTable<?>, List<?>>> resultMap = run.getDataTables().entrySet().stream()
                .filter(entry -> entry.getKey().getName().contains("SourcesFileResults"))
                .findFirst();
            Assertions.assertTrue(resultMap.isPresent());

            List<SourcesFileResults.Row> rows = (List<SourcesFileResults.Row>) resultMap.get().getValue();
            Assertions.assertNotNull(rows);
            Assertions.assertEquals(1,rows.size());

            SourcesFileResults.Row row = rows.getFirst();
            Assertions.assertNotNull(row);
            Assertions.assertEquals("src/main/java/dev/snowdrop/Test.java",row.getSourcePath());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
