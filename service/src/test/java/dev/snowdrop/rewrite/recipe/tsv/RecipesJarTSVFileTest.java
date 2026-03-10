package dev.snowdrop.rewrite.recipe.tsv;

import dev.snowdrop.rewrite.BaseTest;
import dev.snowdrop.rewrite.service.RewriteService;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DataTable;
import org.openrewrite.RecipeRun;
import org.openrewrite.maven.table.DependenciesInUse;
import org.openrewrite.table.SearchResults;
import org.wildfly.common.Assert;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class RecipesJarTSVFileTest extends BaseTest {

    //@Disabled
    @Test
    void useRecipesJarToUpgradeDependencies() throws Exception {

        String appPath = "../test-project/spring-boot-todo-app";
        String recipeName = "org.openrewrite.quarkus.spring.SpringBootToQuarkus";

        // Configure the application to scan and recipe to be executed
        cfg.setAppPath(Paths.get(appPath));
        cfg.setFqNameRecipe(recipeName);
        cfg.setAdditionalJarPaths(List.of(
                "org.openrewrite.recipe:rewrite-spring-to-quarkus:0.6.0",
                "org.openrewrite.recipe:rewrite-java-dependencies:1.51.0"
        ));

        RewriteService rewriteService = new RewriteService(cfg);
        rewriteService.init();
        var results = rewriteService.runScanner();

        RecipeRun run = results.getRecipeRuns().get(recipeName);
        List<DependenciesInUse.Row> rows = findDataTableRows(run, "DependenciesInUse", DependenciesInUse.Row.class);
        assertEquals(46, rows.size());

        DependenciesInUse.Row record = rows.getFirst();
        assertEquals("spring-boot-todo-app", record.getProjectName());
        assertEquals("main", record.getSourceSet());
        assertEquals("org.springframework.boot", record.getGroupId());
        assertEquals("spring-boot-starter-data-jpa", record.getArtifactId());
        assertEquals("3.5.3", record.getVersion());
    }
}