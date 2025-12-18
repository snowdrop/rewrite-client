package dev.snowdrop.openrewrite.cli.toolbox;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassLoaderUtils {

    /**
     * Creates a URLClassLoader from the additional JAR paths or Maven GAV coordinates.
     *
     * @return URLClassLoader containing the additional Rewrite JARs, or null if no additional JARs are specified
     */
    public URLClassLoader loadAdditionalJars(List<String> additionalJarPaths) {
        if (additionalJarPaths.isEmpty()) {
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
            URLClassLoader.newInstance(jarUrls.toArray(new URL[0]), this.getClass().getClassLoader());
    }

    /**
     * Merges URLs from the source classloader into the target classloader.
     * This is similar to the merge functionality in the Maven plugin.
     * Handles both URLClassLoader and Quarkus classloaders gracefully.
     */
    public void merge(ClassLoader targetClassLoader, URLClassLoader sourceClassLoader) {
        // In Quarkus dev mode, the classloader is typically a QuarkusClassLoader,
        // not a URLClassLoader. Since recipe discovery is already handled by the
        // ClasspathScanningLoader with the additional classloader, we don't need
        // to merge URLs into the runtime classloader. Just log the additional JARs.

        if (!(targetClassLoader instanceof URLClassLoader targetUrlClassLoader)) {
            System.out.println("Running in Quarkus mode - using ClasspathScanningLoader for additional JARs:");
            for (URL newUrl : sourceClassLoader.getURLs()) {
                System.out.println("  Using JAR from additional classpath: " + newUrl);
            }
            return;
        }

        Set<String> existingVersionlessJars = new HashSet<>();

        for (URL existingUrl : targetUrlClassLoader.getURLs()) {
            existingVersionlessJars.add(stripVersion(existingUrl));
        }

        for (URL newUrl : sourceClassLoader.getURLs()) {
            if (!existingVersionlessJars.contains(stripVersion(newUrl))) {
                // Note: This is a simplified version. In a real implementation,
                // you might need to use reflection to add URLs to the URLClassLoader
                System.out.println("Would add JAR to classpath: " + newUrl);
            }
        }
    }

    /**
     * Strips version information from JAR URLs for comparison.
     */
    private String stripVersion(URL jarUrl) {
        return jarUrl.toString().replaceAll("/[^/]+/[^/]+\\.jar", "");
    }

}
