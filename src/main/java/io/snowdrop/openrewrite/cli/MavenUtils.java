package io.snowdrop.openrewrite.cli;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.*;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.DependencyCollector;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.ArtifactResolver;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.eclipse.aether.internal.impl.DefaultArtifactResolver;
import org.eclipse.aether.internal.impl.collect.DefaultDependencyCollector;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.jboss.logging.Logger;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MavenUtils {
    private static final Logger logger = Logger.getLogger(MavenUtils.class);

    private ModelBuilder modelBuilder;

    private ModelBuildingResult buildModel(File pomPath) {
        RepositoryModelResolver repositoryModelResolver = new RepositoryModelResolver();
        DefaultModelBuildingRequest req = new DefaultModelBuildingRequest();
        req.setProcessPlugins(false);
        req.setPomFile(pomPath);
        req.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        req.setSystemProperties(System.getProperties());
        req.setLocationTracking(true);
        req.setModelResolver(repositoryModelResolver);

        ModelBuildingResult result = null;
        try {
            return modelBuilder.build(req);
        } catch (Exception e) {
            logger.error("Could not build effective model: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    public Model setupProject(File pomFile) {
        modelBuilder = new DefaultModelBuilderFactory().newInstance();
        ModelBuildingResult modelBuilding = buildModel(pomFile);
        return modelBuilding.getEffectiveModel();
    }

    public static Set<Artifact> convertDependenciesToArtifacts(List<Dependency> dependencies) {
        return dependencies.stream()
            .map(dep -> {
                return new DefaultArtifact(dep.getGroupId(),dep.getArtifactId(),dep.getVersion(),dep.getScope(),dep.getType(),dep.getClassifier(),new DefaultArtifactHandler(dep.getType()));
            })
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
    }
    
    /*
     This is the recommended object for Aether resolution as it retains the scope and optionality.

     @param modelDependencies The list of org.apache.maven.model.Dependency.
     @return A list of org.eclipse.aether.graph.Dependency objects.
     */
    public List<org.eclipse.aether.graph.Dependency> convertModelDependenciesToAetherDependencies(List<Dependency> modelDependencies) {
        return modelDependencies.stream()
            .map(dep -> {
                // 1. Create the Aether Artifact from the model dependency
                String type = (dep.getType() != null && !dep.getType().isEmpty()) ? dep.getType() : "jar";
                String classifier = (dep.getClassifier() != null && !dep.getClassifier().isEmpty()) ? dep.getClassifier() : "";

                // Construct the coordinate string: G:A:T[:C]:V
                String coords;
                if (classifier.isEmpty()) {
                    // Format: G:A:E:V (no classifier)
                    coords = String.format("%s:%s:%s:%s",
                        dep.getGroupId(),
                        dep.getArtifactId(),
                        type,
                        dep.getVersion());
                } else {
                    // Format: G:A:E:C:V (with classifier)
                    coords = String.format("%s:%s:%s:%s:%s",
                        dep.getGroupId(),
                        dep.getArtifactId(),
                        type,
                        classifier,
                        dep.getVersion());
                }

                org.eclipse.aether.artifact.Artifact aetherArtifact;
                try {
                    // Use the Aether DefaultArtifact constructor for robust parsing
                    aetherArtifact = new org.eclipse.aether.artifact.DefaultArtifact(coords);
                } catch (IllegalArgumentException e) {
                    System.err.println("Skipping invalid dependency coordinates during Aether Dependency creation: " + coords);
                    return null;
                }

                // 2. Create the Aether Dependency object
                // Default scope is 'compile' if not specified in the model
                String scope = (dep.getScope() != null && !dep.getScope().isEmpty()) ? dep.getScope() : "compile";
                boolean optional = dep.isOptional();

                // Create Aether Dependency with artifact, scope, and optionality
                return new org.eclipse.aether.graph.Dependency(aetherArtifact, scope, optional);
            })
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());
    }

    // ========== Repository system factory methods ==========

    /**
     * Creates a properly configured RepositorySystem with all required Maven components.
     * This method can be used by other classes to get consistent repository system configuration.
     *
     * @return properly wired RepositorySystem
     */
    public static RepositorySystem createRepositorySystem() {
        // Create service locator with all required Maven repository components
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();

        // Add transport and connector services
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);

        // Add Maven-specific services for artifact and dependency resolution
        locator.addService(DependencyCollector.class, DefaultDependencyCollector.class);
        locator.addService(ArtifactResolver.class, DefaultArtifactResolver.class);

        // Add error handling for debugging
        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                logger.error("Failed to create service: " + type.getName() + " with impl: " + impl.getName());
                exception.printStackTrace();
            }
        });

        return locator.getService(RepositorySystem.class);
    }

    /**
     * Creates a properly configured repository session.
     *
     * @param repositorySystem the repository system to use for creating the session
     * @return configured repository session
     */
    public static DefaultRepositorySystemSession createRepositorySession(RepositorySystem repositorySystem) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        // Set up local repository
        String userHome = System.getProperty("user.home");
        LocalRepository localRepo = new LocalRepository(userHome + "/.m2/repository");
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepo));

        return session;
    }

    /**
     * Creates default remote repositories (Maven Central and Sonatype snapshots).
     *
     * @return list of default remote repositories
     */
    public static List<RemoteRepository> createDefaultRemoteRepositories() {
        return Arrays.asList(
            new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/").build(),
            new RemoteRepository.Builder("sonatype-snapshots", "default", "https://oss.sonatype.org/content/repositories/snapshots/").build()
        );
    }

}
