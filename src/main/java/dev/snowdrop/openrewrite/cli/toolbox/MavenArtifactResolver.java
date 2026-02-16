/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.snowdrop.openrewrite.cli.toolbox;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.mima.extensions.mmr.MavenModelReader;
import eu.maveniverse.maven.mima.extensions.mmr.ModelRequest;
import org.apache.maven.model.Model;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.*;

import java.io.Closeable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for resolving Maven artifacts from GAV (Group:Artifact:Version) coordinates.
 * Supports downloading artifacts from Maven repositories and caching them locally.
 */
public class MavenArtifactResolver implements Closeable {

    // Pattern to match GAV coordinates: group:artifact:version
    private static final Pattern GAV_PATTERN = Pattern.compile("^[a-zA-Z0-9_.\\-]+:[a-zA-Z0-9_.\\-]+:[a-zA-Z0-9_.\\-]+(?:-[a-zA-Z0-9_.\\-]+)*$");

    private final Context context;
    private final MavenModelReader mavenModelReader;
    private final RepositorySystem repositorySystem;
    private final RepositorySystemSession session;
    private final List<RemoteRepository> repositories;

    public MavenArtifactResolver() {
        this.context = Runtimes.INSTANCE.getRuntime().create(ContextOverrides.create().withUserSettings(true).build());
        this.mavenModelReader = new MavenModelReader(context);
        this.repositorySystem = context.repositorySystem();
        this.session = context.repositorySystemSession();
        this.repositories = context.remoteRepositories();
    }

    @Override
    public void close() {
        context.close();
    }

    public Model loadModel(Path pomPath) {
        try {
            return mavenModelReader.readModel(ModelRequest.builder().setPomFile(pomPath).build()).getEffectiveModel();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read POM model from " + pomPath + ": " + e.getMessage(), e);
        }
    }

    /**
     * Determines if a string represents a Maven GAV coordinate or a file path.
     *
     * @param jarPathOrCoordinate the string to check
     * @return true if it's a GAV coordinate, false if it's a file path
     */
    public static boolean isGavCoordinate(String jarPathOrCoordinate) {
        if (jarPathOrCoordinate == null || jarPathOrCoordinate.trim().isEmpty()) {
            return false;
        }

        // If it contains file separators, it's likely a file path
        if (jarPathOrCoordinate.contains("/") || jarPathOrCoordinate.contains("\\")) {
            return false;
        }

        // If it ends with .jar, it's likely a file path
        if (jarPathOrCoordinate.toLowerCase().endsWith(".jar")) {
            return false;
        }

        // Check if it matches the GAV pattern (group:artifact:version)
        return GAV_PATTERN.matcher(jarPathOrCoordinate.trim()).matches();
    }

    /**
     * Resolves a list of jar paths or GAV coordinates to actual file paths.
     *
     * @param jarPathsOrCoordinates list of file paths or GAV coordinates
     * @return list of resolved file paths
     * @throws ArtifactResolutionException if artifact resolution fails
     */
    public List<Path> resolveArtifacts(List<String> jarPathsOrCoordinates) throws ArtifactResolutionException {
        List<Path> resolvedPaths = new ArrayList<>();

        for (String jarPathOrCoordinate : jarPathsOrCoordinates) {
            if (isGavCoordinate(jarPathOrCoordinate)) {
                System.out.println("Resolving Maven artifact: " + jarPathOrCoordinate);
                Path resolvedPath = resolveArtifact(jarPathOrCoordinate);
                resolvedPaths.add(resolvedPath);
                System.out.println("Resolved to: " + resolvedPath);
            } else {
                // It's a file path, use as-is
                resolvedPaths.add(Paths.get(jarPathOrCoordinate));
            }
        }

        return resolvedPaths;
    }

    /**
     * Resolves a single Maven GAV coordinate to a local file path.
     *
     * @param gavCoordinate the GAV coordinate (group:artifact:version)
     * @return the path to the resolved jar file
     * @throws ArtifactResolutionException if artifact resolution fails
     */
    public Path resolveArtifact(String gavCoordinate) throws ArtifactResolutionException {
        String[] parts = gavCoordinate.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid GAV coordinate: " + gavCoordinate +
                    ". Expected format: group:artifact:version");
        }

        String groupId = parts[0];
        String artifactId = parts[1];
        String version = parts[2];

        Artifact artifact = new DefaultArtifact(groupId, artifactId, "jar", version);
        ArtifactRequest request = new ArtifactRequest(artifact, repositories, null);

        // session.setOffline(true);
        ArtifactResult result = repositorySystem.resolveArtifact(session, request);
        return result.getArtifact().getFile().toPath();
    }

    /**
     * Resolves artifacts with their transitive dependencies.
     *
     * @param model The Maven model
     * @return list of resolved file paths including transitive dependencies
     * @throws DependencyResolutionException if dependency resolution fails
     */
    public List<Path> resolveArtifactsWithDependencies(Model model) {
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setDependencies(convertModelDependencyToAetherDependency(model.getDependencies()));

        List<Dependency> managedDependencies = Optional.ofNullable(model.getDependencyManagement())
                .map(dm -> convertModelDependencyToAetherDependency(dm.getDependencies()))
                .orElse(Collections.emptyList());
        collectRequest.setManagedDependencies(managedDependencies);
        collectRequest.setRepositories(repositories);

        List<Path> resolvedPaths = new ArrayList<>();
        try {
            DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);
            DependencyResult dependencyResult = repositorySystem.resolveDependencies(session, dependencyRequest);

            for (ArtifactResult artifactResult : dependencyResult.getArtifactResults()) {
                resolvedPaths.add(artifactResult.getArtifact().getFile().toPath());
            }
        } catch (DependencyResolutionException ex) {
            throw new RuntimeException("Could not resolve dependencies: " + ex.getMessage(), ex);
        }
        return resolvedPaths;
    }

    /*
     This is the recommended object for Aether resolution as it retains the scope and optionality.

     @param modelDependencies The list of org.apache.maven.model.Dependency.
     @return A list of org.eclipse.aether.graph.Dependency objects.
     */
    public static List<org.eclipse.aether.graph.Dependency> convertModelDependencyToAetherDependency(List<org.apache.maven.model.Dependency> modelDependencies) {
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
}