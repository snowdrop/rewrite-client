package dev.snowdrop.rewrite.recipe.fqn;

import dev.snowdrop.rewrite.cli.BaseTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.openrewrite.RecipeRun;
import org.wildfly.common.Assert;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class AdditionalRecipesJarTest extends BaseTest {

    @Test
    void testAdditionalRecipesJarToUpgradeDependencyVersion() throws Exception {

        String appPath = "test-project/spring-boot-app";
        Path rewritePatchFile = Paths.get(appPath, "target/rewrite/rewrite.patch");
        String recipeName = "org.openrewrite.java.dependencies.UpgradeDependencyVersion";

        // Configure the application to scan and recipe to be executed
        cfg.setAppPath(Paths.get(appPath));
        cfg.setFqNameRecipe(recipeName);
        cfg.setRecipeOptions(Set.of("groupId=org.springframework.boot","artifactId=*","newVersion=3.5.10","overrideManagedVersion=true"));
        cfg.setAdditionalJarPaths(List.of("org.openrewrite.recipe:rewrite-java-dependencies:1.51.0"));

        var results = rewriteCmd.execute(cfg);
        RecipeRun run = results.getRecipeRuns().get(recipeName);
        Assert.assertNotNull(run);

        assertFalse(run.getDataTables().isEmpty());

        String patchContent = Files.readString(rewritePatchFile);
        assertNotNull(patchContent);

        String versionChanged = "<version>3.5.10</version>";
        boolean found = patchContent.contains(versionChanged);
        assertTrue(found, "The rewrite patch file contains the string :" + versionChanged);
    }
}