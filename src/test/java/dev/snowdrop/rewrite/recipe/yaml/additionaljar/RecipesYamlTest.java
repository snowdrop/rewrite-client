package dev.snowdrop.rewrite.recipe.yaml.additionaljar;

import dev.snowdrop.rewrite.cli.BaseTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.wildfly.common.Assert;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class RecipesYamlTest extends BaseTest {

    @Test
    void useAdditionalRecipesJarToUpgradeDependencies() throws Exception {

        String appPath = "test-project/demo-spring-boot-todo-app";
        String recipeName = "dev.snowdrop.RecipeExample";

        // Configure the application to scan and recipe to be executed
        cfg.setAppPath(Paths.get(appPath));
        cfg.setFqNameRecipe(recipeName);
        cfg.setAdditionalJarPaths(List.of("org.openrewrite.recipe:rewrite-java-dependencies:1.51.0"));
        cfg.setYamlRecipesPath("rewrite-upgrade-dependencies-version.yml");

        var results = rewriteCmd.execute(cfg);
        RecipeRun run = results.getRecipeRuns().get(recipeName);

        Assert.assertTrue(run.getChangeset().getAllResults().size() == 3);
        List<Result> changes = run.getChangeset().getAllResults();

    }
}