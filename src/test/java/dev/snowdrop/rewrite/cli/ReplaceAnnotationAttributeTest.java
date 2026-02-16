package dev.snowdrop.rewrite.cli;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.DataTable;
import org.openrewrite.RecipeRun;
import org.openrewrite.table.SearchResults;
import org.openrewrite.table.SourcesFileResults;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ReplaceAnnotationAttributeTest extends BaseTest {

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
    void shouldChangeGeneratedValuetoIdentity() throws Exception {
        String recipeName = "org.openrewrite.java.AddOrUpdateAnnotationAttribute";

        // Configure the application to scan and recipe to be executed
        cfg.setAppPath(Paths.get(appPath));
        cfg.setFqNameRecipe(recipeName);
        cfg.setRecipeOptions(Set.of("annotationType=jakarta.persistence.GeneratedValue","attributeName=strategy","attributeValue=IDENTITY"));

        var results = rewriteCmd.execute(cfg);
        RecipeRun run = results.getRecipeRuns().get(recipeName);

        assertFalse(run.getDataTables().isEmpty());
        Optional<Map.Entry<DataTable<?>, List<?>>> resultMap = run.getDataTables().entrySet().stream()
                .filter(entry -> entry.getKey().getName().contains("SourcesFileResults"))
                .findFirst();
        assertTrue(resultMap.isPresent());

        List<?> rows = resultMap.get().getValue();
        assertEquals(1, rows.size());

        SourcesFileResults.Row record = (SourcesFileResults.Row) rows.getFirst();
        assertEquals("src/main/java/com/todo/app/entity/Task.java", record.getSourcePath());
        assertEquals("org.openrewrite.java.AddOrUpdateAnnotationAttribute", record.getRecipe());
    }

}