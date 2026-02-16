package dev.snowdrop.rewrite.cli;

import dev.snowdrop.openrewrite.cli.toolbox.MavenArtifactResolver;
import dev.snowdrop.openrewrite.cli.toolbox.MavenUtils;
import org.apache.maven.model.Model;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.util.graph.visitor.DependencyGraphDumper;
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