package dev.snowdrop.rewrite.recipe.fqn;

import dev.snowdrop.rewrite.cli.BaseTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
public class FindPropertiesKeyTest extends BaseTest {

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
    void shouldFindJpaPropertiesKeyExactly() throws Exception {
        Path rewritePatchFile = Paths.get(appPath, "target/rewrite/rewrite.patch");
        String recipeName = "org.openrewrite.properties.search.FindProperties";

        // Configure the application to scan and recipe to be executed
        cfg.setAppPath(Paths.get(appPath));
        cfg.setFqNameRecipe(recipeName);
        cfg.setRecipeOptions(Set.of("propertyKey=spring.jpa.hibernate.ddl-auto","relaxedBinding=false"));

        var results = rewriteCmd.execute(cfg);
        RecipeRun run = results.getRecipeRuns().get(recipeName);

        String patchContent = Files.readString(rewritePatchFile);
        assertNotNull(patchContent);

        List<SearchResults.Row> rows = findDataTableRows(run, "SearchResults", SearchResults.Row.class);
        assertEquals(1, rows.size());

        SearchResults.Row record = rows.getFirst();
        assertEquals("src/main/resources/application.properties",record.getSourcePath());
        assertEquals("spring.jpa.hibernate.ddl-auto=update",record.getResult());
        assertEquals("Find property `spring.jpa.hibernate.ddl-auto`",record.getRecipe());
    }

    @Test
    void shouldFindJpaPropertiesKeyRelaxedBinding() throws Exception {
        String appPath = "test-project/demo-spring-boot-todo-app";
        Path rewritePatchFile = Paths.get(appPath, "target/rewrite/rewrite.patch");
        String recipeName = "org.openrewrite.properties.search.FindProperties";

        // Configure the application to scan and recipe to be executed
        cfg.setAppPath(Paths.get(appPath));
        cfg.setFqNameRecipe(recipeName);
        cfg.setRecipeOptions(Set.of("propertyKey=spring.jpa.hibernate.ddlAuto","relaxedBinding=true"));

        var results = rewriteCmd.execute(cfg);
        RecipeRun run = results.getRecipeRuns().get(recipeName);

        String patchContent = Files.readString(rewritePatchFile);
        assertNotNull(patchContent);

        List<SearchResults.Row> rows = findDataTableRows(run, "SearchResults", SearchResults.Row.class);
        assertEquals(1, rows.size());

        SearchResults.Row record = rows.getFirst();
        assertEquals("src/main/resources/application.properties",record.getSourcePath());
        assertEquals("spring.jpa.hibernate.ddl-auto=update",record.getResult());
        assertEquals("Find property `spring.jpa.hibernate.ddlAuto`",record.getRecipe());
    }
}