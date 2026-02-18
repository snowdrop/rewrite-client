package dev.snowdrop.rewrite.recipe.fqn;

import dev.snowdrop.rewrite.cli.BaseTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.DataTable;
import org.openrewrite.RecipeRun;
import org.openrewrite.table.SearchResults;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class FindAnnotationTest extends BaseTest {

    String appPath = "test-project/demo-spring-boot-todo-app";

    @BeforeEach
    public void setup() throws IOException {
        Path pathToBeDeleted = Paths.get(appPath, "target/rewrite");
        if (Files.exists(pathToBeDeleted)) {
            try (Stream<Path> paths = Files.walk(pathToBeDeleted)) {
                paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    @Test
    void shouldFindAnnotation() throws Exception {
        String recipeName = "org.openrewrite.java.search.FindAnnotations";

        // Configure the application to scan and recipe to be executed
        cfg.setAppPath(Paths.get(appPath));
        cfg.setFqNameRecipe(recipeName);
        cfg.setRecipeOptions(Set.of("annotationPattern=org.springframework.boot.autoconfigure.SpringBootApplication","matchMetaAnnotations=false"));

        var results = rewriteCmd.execute(cfg);
        RecipeRun run = results.getRecipeRuns().get(recipeName);

        assertFalse(run.getDataTables().isEmpty());
        Optional<Map.Entry<DataTable<?>, List<?>>> resultMap = run.getDataTables().entrySet().stream()
            .filter(entry -> entry.getKey().getName().contains("SearchResults"))
            .findFirst();
        assertTrue(resultMap.isPresent());

        List<?> rows = resultMap.get().getValue();
        assertEquals(1, rows.size());

        SearchResults.Row record = (SearchResults.Row) rows.getFirst();
        assertEquals("src/main/java/com/todo/app/AppApplication.java", record.getSourcePath());
        assertEquals("@SpringBootApplication", record.getResult());
        assertEquals("Find annotations `org.springframework.boot.autoconfigure.SpringBootApplication`", record.getRecipe());
    }
}