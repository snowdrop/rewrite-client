package dev.snowdrop.rewrite.recipe.yaml.springtoquarkus;

import dev.snowdrop.rewrite.cli.BaseTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.wildfly.common.Assert;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class UpgradeDepVersionYamlTest extends BaseTest {

    @Test
    void testSpringToQuarkusMigration() throws Exception {

        String appPath = "test-project/spring-boot-todo-app";
        String recipeName = "dev.snowdrop.openrewrite.java.SpringToQuarkus";

        List<String> gavs = new ArrayList<>();
        gavs.add("org.openrewrite.recipe:rewrite-java-dependencies:1.51.0");

        // Configure the application to scan and recipe to be executed
        cfg.setAppPath(Paths.get(appPath));
        cfg.setFqNameRecipe(recipeName);
        cfg.setYamlRecipesPath("rewrite-specific-springboot.yml");
        cfg.setAdditionalJarPaths(gavs);

        var results = rewriteCmd.execute(cfg);
        RecipeRun run = results.getRecipeRuns().get(recipeName);

        Assert.assertTrue(run.getChangeset().getAllResults().size() == 1);
        List<Result> changes = run.getChangeset().getAllResults();

        /*
            EXPECT: Version replaced in: pom.xml
             - <version>3.5.3</version>
             + <version>3.5.8</version>
         */
        assertNotNull(changes.get(0).getAfter());
        assertEquals("pom.xml", changes.get(0).getAfter().getSourcePath().toString());

    }
}