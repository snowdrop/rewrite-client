package dev.snowdrop.rewrite.cli;

import dev.snowdrop.openrewrite.cli.RewriteCommand;
import io.quarkus.test.junit.QuarkusTest;
import org.jetbrains.kotlin.com.intellij.openapi.util.text.Strings;
import org.junit.jupiter.api.Test;
import org.openrewrite.DataTable;
import org.openrewrite.RecipeRun;
import org.openrewrite.table.SearchResults;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class FindAnnotation extends BaseTest {

    @Test
    void shouldFindAnnotation() throws Exception {
        RewriteCommand rewriteCmd = new RewriteCommand();

        String appPath = "test-project/demo-spring-boot-todo-app";
        Path rewritePatchFile = Paths.get(appPath, "target/rewrite/rewrite.patch");
        String recipeName = "org.openrewrite.java.search.FindAnnotations";

        // Configure the application to scan and recipe to be executed
        cfg.setAppPath(Paths.get(appPath));
        cfg.setActiveRecipes(List.of(recipeName));
        cfg.setRecipeOptions(Set.of("annotationPattern=@org.springframework.boot.autoconfigure.SpringBootApplication,matchMetaAnnotations=false"));

        var results = rewriteCmd.execute(cfg);
        RecipeRun run = results.getRecipeRuns().get(recipeName);

        String patchContent = Files.readString(rewritePatchFile);
        assertNotNull(patchContent);

        assertFalse(run.getDataTables().isEmpty());
        Optional<Map.Entry<DataTable<?>, List<?>>> resultMap = run.getDataTables().entrySet().stream()
            .filter(entry -> entry.getKey().getName().contains("SearchResults"))
            .findFirst();
        assertTrue(resultMap.isPresent());

        List<?> rows = resultMap.get().getValue();
        assertEquals(1, rows.size());

        SearchResults.Row record = (SearchResults.Row)rows.getFirst();
        assertEquals("src/main/java/com/todo/app/AppApplication.java",record.getSourcePath());
        assertEquals("@SpringBootApplication",record.getResult());
        assertEquals("Find annotations `@org.springframework.boot.autoconfigure.SpringBootApplication,matchMetaAnnotations=false`",record.getRecipe());
    }
}