package dev.snowdrop.rewrite.recipe.yaml.simple;

import dev.snowdrop.rewrite.cli.BaseTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.openrewrite.RecipeRun;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class RecipesYamlTest extends BaseTest {

    @Test
    void testAutoFormatRecipeYaml() throws Exception {

        String appPath = "test-project/simple";
        Path rewritePatchFile = Paths.get(appPath, "target/rewrite/rewrite.patch");
        String recipeName = "dev.snowdrop.RecipeExample";

        // Configure the application to scan and recipe to be executed
        cfg.setAppPath(Paths.get(appPath));
        cfg.setFqNameRecipe(recipeName);
        cfg.setYamlRecipesPath("rewrite.yml");

        var results = rewriteCmd.execute(cfg);
        RecipeRun run = results.getRecipeRuns().get(recipeName);
        assertFalse(run.getDataTables().isEmpty());

        String patchContent = Files.readString(rewritePatchFile);
        assertNotNull(patchContent);

        String diffText = """
            -public class Test { }
            +public class Test {
            +}
            """;

        boolean found = patchContent.contains(diffText);
        assertTrue(found, "The rewrite patch file contains the string :" + diffText);
    }
}