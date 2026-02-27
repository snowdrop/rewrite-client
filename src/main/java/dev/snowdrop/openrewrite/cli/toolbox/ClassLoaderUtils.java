package dev.snowdrop.openrewrite.cli.toolbox;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

/**
 * Utility methods for loading additional JARs and merging classloaders.
 */
public class ClassLoaderUtils {
    Logger LOG = Logger.getLogger(ClassLoaderUtils.class.getName());


    /**
     * Creates a new ClassLoaderUtils instance.
     */
    public ClassLoaderUtils() {
    }

    /**
     * Resolves additional JAR paths or Maven GAV coordinates to a list of local file paths.
     *
     * @param additionalJarPaths list of JAR file paths or Maven GAV coordinates to resolve
     * @return list of resolved JAR file paths, empty if no additional JARs are specified
     */
    public List<Path> resolveAdditionalJarPaths(List<String> additionalJarPaths) {
        if (additionalJarPaths == null || additionalJarPaths.isEmpty()) {
            return List.of();
        }

        MavenArtifactResolver resolver = new MavenArtifactResolver();
        try {
            List<Path> resolvedPaths = resolver.resolveArtifacts(additionalJarPaths);
            List<Path> validPaths = new ArrayList<>();
            for (Path jarPath : resolvedPaths) {
                if (!Files.exists(jarPath)) {
                    System.err.println("Warning: JAR file does not exist: " + jarPath);
                    continue;
                }
                if (!jarPath.toString().toLowerCase().endsWith(".jar")) {
                    System.err.println("Warning: File is not a JAR: " + jarPath);
                    continue;
                }
                validPaths.add(jarPath);
                System.out.println("Resolved additional JAR: " + jarPath);
            }
            return validPaths;
        } catch (Exception e) {
            System.err.println("Error resolving Maven artifacts: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }


    /**
     * Creates a URLClassLoader from the additional JAR paths or Maven GAV coordinates.
     *
     * @param additionalJarPaths list of JAR file paths or Maven GAV coordinates to load
     * @return URLClassLoader containing the additional Rewrite JARs, or null if no additional JARs are specified
     */
    public URLClassLoader loadAdditionalJars(List<String> additionalJarPaths) {
        if (additionalJarPaths == null || additionalJarPaths.isEmpty()) {
            return null;
        }

        List<URL> jarUrls = new ArrayList<>();
        MavenArtifactResolver resolver = new MavenArtifactResolver();

        try {
            // Resolve all jar paths/coordinates to actual file paths
            List<Path> resolvedPaths = resolver.resolveArtifacts(additionalJarPaths);

            for (Path jarPath : resolvedPaths) {
                try {
                    if (!Files.exists(jarPath)) {
                        System.err.println("Warning: JAR file does not exist: " + jarPath);
                        continue;
                    }
                    if (!jarPath.toString().toLowerCase().endsWith(".jar")) {
                        System.err.println("Warning: File is not a JAR: " + jarPath);
                        continue;
                    }
                    jarUrls.add(jarPath.toUri().toURL());
                    System.out.println("Loaded additional JAR: " + jarPath);
                } catch (MalformedURLException e) {
                    System.err.println("Could not load JAR: " + jarPath + " - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error resolving Maven artifacts: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        return jarUrls.isEmpty() ? null :
                URLClassLoader.newInstance(jarUrls.toArray(URL[]::new), this.getClass().getClassLoader());
    }
}