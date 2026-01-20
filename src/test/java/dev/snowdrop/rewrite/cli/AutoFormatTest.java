package dev.snowdrop.rewrite.cli;

import dev.snowdrop.openrewrite.cli.RewriteCommand;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.openrewrite.RecipeRun;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class AutoFormatTest extends BaseTest {

    @Test
    void testAutoFormatRecipe() throws Exception {
        RewriteCommand rewriteCmd = new RewriteCommand();

        String appPath = "test-project/simple";
        Path rewritePatchFile = Paths.get(appPath, "target/rewrite/rewrite.patch");
        String recipeName = "org.openrewrite.java.format.AutoFormat";

        // Configure the application to scan and recipe to be executed
        cfg.setAppPath(Paths.get(appPath));
        cfg.setFqNameRecipe(recipeName);

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