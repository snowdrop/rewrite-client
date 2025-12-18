package dev.snowdrop.rewrite.cli;

import dev.snowdrop.openrewrite.cli.RewriteCommand;
import dev.snowdrop.openrewrite.cli.RewriteConfiguration;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
public class CommandTest {

    RewriteCommand rewriteCmd = new RewriteCommand();

    @Test
    void testCommandSuccess() {
        CommandLine cmd = new CommandLine(rewriteCmd);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute("test-project/simple", "-r", "org.openrewrite.java.format.AutoFormat");

        Assertions.assertEquals(0, exitCode, "TODO");
        assertThat(sw.toString(), containsString("Recipe applied successfully"));
    }
}