package dev.snowdrop.rewrite.cli;

import dev.snowdrop.openrewrite.cli.toolbox.MavenArtifactResolver;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dev.snowdrop.openrewrite.cli.toolbox.MavenArtifactResolver.convertModelDependencyToAetherDependency;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DependencyNodeIssue {

    static String appPath = "test-project/quarkus-resteasy-classic-app";
    public static final DependencyGraphDumper DUMPER_SOUT = new DependencyGraphDumper(System.out::println);

    @Test
    void failToGetAllJars() {
        List<Path> classpaths = resolveProjectDependencies(appPath);
        System.out.println("Classpath jar entries size: " + classpaths.size());
        classpaths.forEach(cp -> System.out.println(cp.toString()));
        assertEquals(268, classpaths.size(), "Expected 268 jars in the classpath");
    }

    List<Path> resolveProjectDependencies(String appPath) {
        try (MavenArtifactResolver mar = new MavenArtifactResolver()) {
            return mar.resolveArtifactsWithDependencies(mar.loadModel(Paths.get(appPath, "pom.xml")));
        }
    }

    @Test
    void shouldGetAllJars() throws DependencyCollectionException {
        RepositorySystem system = new RepositorySystemSupplier().get();
        RepositorySystemSession session = newRepositorySystemSession(system);

        Model model;
        try (MavenArtifactResolver mar = new MavenArtifactResolver()) {
            model = mar.loadModel(Paths.get(appPath, "pom.xml"));
        }

        // Print the GAVS of the pom.xml
        model.getDependencies().forEach(System.out::println);

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRepositories(newRepositories(system, session));
        collectRequest.setDependencies(convertModelDependencyToAetherDependency(model.getDependencies()));
        CollectResult collectResult = system.collectDependencies(session, collectRequest);
        collectResult.getRoot().accept(DUMPER_SOUT);
    }

    public static DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(appPath);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        return session;
    }

    public static List<RemoteRepository> newRepositories(RepositorySystem system, RepositorySystemSession session) {
        return new ArrayList<>(Collections.singletonList(newCentralRepository()));
    }

    private static RemoteRepository newCentralRepository() {
        return new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build();
    }
}