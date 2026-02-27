package dev.snowdrop.rewrite.recipe.yaml.additionaljar;

import dev.snowdrop.rewrite.cli.BaseTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.openrewrite.RecipeRun;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class RecipesYamlTest extends BaseTest {

    @Test
    void useAdditionalRecipesJarToUpgradeDependencies() throws Exception {

        String appPath = "test-project/spring-boot-todo-app";
        String recipeName = "dev.snowdrop.RecipeExample";

        // Configure the application to scan and recipe to be executed
        cfg.setAppPath(Paths.get(appPath));
        cfg.setFqNameRecipe(recipeName);
        cfg.setAdditionalJarPaths(List.of("org.openrewrite.recipe:rewrite-java-dependencies:1.51.0"));
        cfg.setYamlRecipesPath("rewrite-upgrade-dependencies-version.yml");

        var results = rewriteCmd.execute(cfg);
        RecipeRun run = results.getRecipeRuns().get(recipeName);
        var result = run.getChangeset().getAllResults().getFirst();
        String diff = result.diff();
        String versionChanged = "<version>3.5.10</version>";
        assertTrue(diff.contains(versionChanged));

    }
}