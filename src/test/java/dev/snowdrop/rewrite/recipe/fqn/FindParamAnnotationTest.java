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
public class FindParamAnnotationTest extends BaseTest {

    String appPath = "test-project/quarkus-resteasy-classic-app";

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
    void shouldFindJakartaGetAnnotation() throws Exception {
        String recipeName = "org.openrewrite.java.search.FindAnnotations";
        String annotationToSearch="jakarta.ws.rs.GET";

        // Configure the application to scan and recipe to be executed
        cfg.setAppPath(Paths.get(appPath));
        cfg.setFqNameRecipe(recipeName);
        cfg.setRecipeOptions(Set.of(String.format("annotationPattern=%s",annotationToSearch),"matchMetaAnnotations=false"));
        cfg.setAdditionalJarPaths(List.of(""));

        var results = rewriteCmd.execute(cfg);
        RecipeRun run = results.getRecipeRuns().get(recipeName);

        List<SearchResults.Row> rows = findDataTableRows(run, "SearchResults", SearchResults.Row.class);
        assertEquals(4, rows.size());

        SearchResults.Row record = rows.getFirst();
        assertEquals("src/main/java/com/demo/library/BookResource.java", record.getSourcePath());
        assertEquals("@GET", record.getResult());
        assertEquals("Find annotations `jakarta.ws.rs.GET`", record.getRecipe());
    }

    @Test
    void shouldFindJBossRestEasyPathParamAnnotation() throws Exception {
        String recipeName = "org.openrewrite.java.search.FindAnnotations";
        String annotationToSearch="org.jboss.resteasy.annotations.jaxrs.PathParam";

        // Configure the application to scan and recipe to be executed
        cfg.setAppPath(Paths.get(appPath));
        cfg.setFqNameRecipe(recipeName);
        cfg.setRecipeOptions(Set.of(String.format("annotationPattern=%s",annotationToSearch),"matchMetaAnnotations=false"));

        var results = rewriteCmd.execute(cfg);
        RecipeRun run = results.getRecipeRuns().get(recipeName);

        List<SearchResults.Row> rows = findDataTableRows(run, "SearchResults", SearchResults.Row.class);
        assertEquals(3, rows.size());

        SearchResults.Row record = rows.getFirst();
        assertEquals("src/main/java/com/demo/library/BookResource.java", record.getSourcePath());
        assertEquals(true, record.getResult().contains("@PathParam"));
        assertEquals("Find annotations `org.jboss.resteasy.annotations.jaxrs.PathParam`", record.getRecipe());
    }
}