package dev.snowdrop.rewrite.toolbox;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.jboss.logging.Logger;

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
                    LOG.error("Warning: JAR file does not exist: " + jarPath);
                    continue;
                }
                if (!jarPath.toString().toLowerCase().endsWith(".jar")) {
                    LOG.error("Warning: File is not a JAR: " + jarPath);
                    continue;
                }
                validPaths.add(jarPath);
                LOG.info("Resolved additional JAR: " + jarPath);
            }
            return validPaths;
        } catch (Exception e) {
            LOG.error("Error resolving Maven artifacts: ", e);
            return List.of();
        }
    }


    /**
     * Creates a URLClassLoader from the additional JAR paths or Maven GAV coordinates.
     *
     * @param additionalJarPaths list of JAR file paths or Maven GAV coordinates to load
     * @return URLClassLoader containing the additional Rewrite JARs, or null if no additional JARs are specified
     */
    public URLClassLoader loadAdditionalJars(List<String> additionalJarPaths, ClassLoader rewriteClassLoader) {
        if (additionalJarPaths == null || additionalJarPaths.isEmpty()) {
            return null;
        }

        ClassLoader cl;
        if(rewriteClassLoader == null) {
            cl = this.getClass().getClassLoader();
        } else {
            cl = rewriteClassLoader;
        }

        List<URL> jarUrls = new ArrayList<>();
        MavenArtifactResolver resolver = new MavenArtifactResolver();

        try {
            // Resolve all jar paths/coordinates to actual file paths
            List<Path> resolvedPaths = resolver.resolveArtifacts(additionalJarPaths);

            for (Path jarPath : resolvedPaths) {
                try {
                    if (!Files.exists(jarPath)) {
                        LOG.error("Warning: JAR file does not exist: " + jarPath);
                        continue;
                    }
                    if (!jarPath.toString().toLowerCase().endsWith(".jar")) {
                        LOG.error("Warning: File is not a JAR: " + jarPath);
                        continue;
                    }
                    jarUrls.add(jarPath.toUri().toURL());
                    LOG.info("Loaded additional JAR: " + jarPath);
                } catch (MalformedURLException e) {
                    LOG.error("Could not load JAR: " + jarPath + " - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            LOG.error("Error resolving Maven artifacts",e);
            return null;
        }

        return jarUrls.isEmpty() ? null :
                new URLClassLoader(jarUrls.toArray(new URL[0]),cl);
    }

    public void exportClassLoaderResources(ClassLoader cl, String... filters) throws IOException {
        // 1. Determine a clean filename based on the ClassLoader name
        String loaderName = (cl.getName() != null) ? cl.getName() : "loader-" + cl.hashCode();
        String fileName = loaderName.replaceAll("[^a-zA-Z0-9.-]", "_") + ".txt";

        // 2. Open the file writer
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("Listing resources for ClassLoader: " + loaderName);
            writer.newLine();
            writer.write("Filters applied: " + Arrays.toString(filters));
            writer.newLine();
            writer.write("--------------------------------------------------");
            writer.newLine();

            // 3. Scan roots
            Enumeration<URL> en = cl.getResources("");
            while (en.hasMoreElements()) {
                URL url = en.nextElement();
                URLConnection urlConnection = url.openConnection();

                if (urlConnection instanceof JarURLConnection jarCon) {
                    try (JarFile jar = jarCon.getJarFile()) {
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            String entryName = entries.nextElement().getName();

                            // 4. Filter logic
                            boolean matches = (filters.length == 0) ||
                                    Arrays.stream(filters).anyMatch(entryName::contains);

                            if (matches) {
                                writer.write(entryName);
                                writer.newLine();
                            }
                        }
                    }
                } else {
                    writer.write("[SystemPath] Skipping non-JAR root: " + url);
                    writer.newLine();
                }
            }
            LOG.info("Resource list exported successfully to: " + fileName);
        }
    }
}