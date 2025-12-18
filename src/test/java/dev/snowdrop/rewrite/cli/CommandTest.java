package dev.snowdrop.rewrite.cli;

import dev.snowdrop.openrewrite.cli.RewriteCommand;
import dev.snowdrop.openrewrite.cli.model.Config;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class CommandTest {

    @Test
    void testExecuteWithConfig() throws Exception {
        RewriteCommand rewriteCmd = new RewriteCommand();

        String appPath = "test-project/simple";
        Path rewritePatchFile = Paths.get(appPath, "target/rewrite/rewrite.patch");

        // Create a properly initialized Config object
        Config cfg = new Config();
        cfg.setAppPath(Paths.get(appPath));
        cfg.setActiveRecipes(List.of("org.openrewrite.java.format.AutoFormat"));
        cfg.setExportDatatables(true);
        cfg.setExclusions(Set.of());
        cfg.setPlainTextMasks(Set.of());
        cfg.setAdditionalJarPaths(List.of());

        rewriteCmd.execute(cfg);

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