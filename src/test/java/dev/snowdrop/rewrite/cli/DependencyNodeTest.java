package dev.snowdrop.rewrite.cli;

import dev.snowdrop.openrewrite.cli.toolbox.MavenArtifactResolver;
import dev.snowdrop.openrewrite.cli.toolbox.MavenUtils;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


import static org.junit.jupiter.api.Assertions.assertEquals;

public class DependencyNodeTest {

    static String appPath = "test-project/quarkus-resteasy-classic-app";

    @Test
    void shouldGetAllTransitiveForQuarkusRestEasy() {
        List<Path> classpaths = resolveProjectDependencies(appPath);
        System.out.println("Classpath jar entries size: " + classpaths.size());
        classpaths.forEach(cp -> System.out.println(cp.toString()));
        assertEquals(270, classpaths.size(), "Expected 270 jars in the classpath");
    }

    List<Path> resolveProjectDependencies(String appPath) {
        MavenUtils mavenUtils = new MavenUtils();
        Model model = mavenUtils.setupProject(Paths.get(appPath, "pom.xml").toFile());

        MavenArtifactResolver mar = new MavenArtifactResolver();
        return mar.resolveArtifactsWithDependencies(model);
    }
}