/// usr/bin/env jbang “$0” “$@” ; exit $?
//DEPS dev.snowdrop.openrewrite:rewrite-client:0.2.12-SNAPSHOT
//DEPS io.quarkus.platform:quarkus-bom:3.29.4@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS io.quarkus:quarkus-config-yaml
//DEPS org.openrewrite:rewrite-polyglot:2.9.1
//DEPS org.openrewrite:rewrite-bom:8.69.1@pom
//DEPS org.openrewrite:rewrite-core
//DEPS org.openrewrite:rewrite-java
//DEPS org.openrewrite:rewrite-java-21
//DEPS org.openrewrite:rewrite-kotlin
//DEPS org.openrewrite:rewrite-yaml
//DEPS org.openrewrite:rewrite-xml
//DEPS org.openrewrite:rewrite-properties
//DEPS org.openrewrite:rewrite-json
//DEPS org.openrewrite:rewrite-gradle
//DEPS org.openrewrite:rewrite-maven
//RUNTIME_OPTIONS -Djansi.colors=256

// List of DEPS generated using the command: mvn dependency:list -DexcludeTransitive=true | grep ":.*:.*:.*" | cut -d']' -f2- | sed 's/^ /\/\/DEPS /'

package dev.snowdrop.rewrite.cli;

import dev.snowdrop.rewrite.ResultsContainer;
import dev.snowdrop.rewrite.cli.toolbox.LoggerUtils;
import dev.snowdrop.rewrite.config.RewriteConfig;
import dev.snowdrop.rewrite.service.RewriteService;
//import dev.snowdrop.rewrite.toolbox.ClassLoaderUtils;
import dev.snowdrop.rewrite.toolbox.ClassLoaderUtils;
import dev.snowdrop.rewrite.toolbox.MavenArtifactResolver;
import io.quarkus.logging.Log;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import picocli.CommandLine;

//import java.lang.reflect.Method;
//import java.net.URL;
//import java.net.URLClassLoader;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

/**
 * Quarkus-based standalone CLI for OpenRewrite
 */
@TopCommand
@CommandLine.Command(
        name = "rewrite",
        mixinStandardHelpOptions = true,
        version = "0.2.12-SNAPSHOT",
        description = "Standalone OpenRewrite CLI tool for applying recipe on the code source of an application"
)
public class RewriteCommand implements Runnable {
    @Inject
    RewriteConfiguration rewriteConfiguration;
    private Logger logger = Logger.getLogger(RewriteCommand.class.getName());

    /**
     * Creates a new RewriteCommand instance.
     */
    public RewriteCommand() {
    }

    @ConfigProperty(name = "cli.log.msg.format")
    String logMsgFormat;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @CommandLine.Parameters(
            index = "0",
            description = "The root directory of the project to analyze"
    )
    Path projectRoot;

    @CommandLine.Option(
            names = {"-r", "--recipe"},
            description = "FQName of the Java Recipe class to run (e.g., org.openrewrite.java.format.AutoFormat)",
            required = false
    )
    String recipeName;

    @CommandLine.Option(
            names = {"-o", "--options"},
            description = "Options to be used to set the recipe's object fields. Example: annotationPattern=@SpringBootApplication",
            split = ",",
            required = false
    )
    LinkedHashSet<String> recipeOptions;

    @CommandLine.Option(
            names = {"--jar"},
            description = "Additional JAR files containing recipes (file paths or Maven GAV coordinates, can be specified multiple times or comma-separated)",
            split = ","
    )
    List<String> additionalJarPaths = new ArrayList<>();

    @CommandLine.Option(
            names = {"--config", "-c"},
            description = "Path to the recipes rewrite.yml file (default: ${DEFAULT-VALUE})"
    )
    String yamlRecipesPath;

    @CommandLine.Option(
            names = {"--export-datatables"},
            description = "Export datatables to CSV files",
            defaultValue = "true"
    )
    boolean exportDatatables;

    @CommandLine.Option(
            names = {"--exclusions"},
            description = "File patterns to exclude (can be specified multiple times)",
            split = ","
    )
    Set<String> exclusions = new HashSet<>();

    @CommandLine.Option(
            names = {"--plain-text-masks"},
            description = "Plain text file masks (can be specified multiple times)",
            split = ","
    )
    Set<String> plainTextMasks = new HashSet<>();

    @CommandLine.Option(
            names = {"--size-threshold-mb"},
            description = "Size threshold in MB for large files (default: ${DEFAULT-VALUE})"
    )
    int sizeThresholdMb = 10;

    @CommandLine.Option(
            names = {"-d", "--dry-run"},
            arity = "1",
            description = "Execute the recipes in dry run mode"
    )
    boolean dryRun = true;

    @CommandLine.Option(
            names = {"-v", "--verbose"},
            description = "Enable verbose output")
    private boolean verbose;

    /*
       The @Inject annotation is disabled and replaced with ConfigProvider.getConfig()
       as we got an Arc error during the execution of the jbang export command
       Unsatisfied dependency for type dev.snowdrop.rewrite.cli.RewriteConfiguration and qualifiers [@Default]
     */
    @Inject
    RewriteConfiguration config;

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        var darken = LoggerUtils.isTerminalDark();
        LoggerUtils.setupLogManagerAndHandler(logMsgFormat, spec, darken);

        try {
            // TODO: To be reviewed as the LogFactory object is created when setSpec is called
            //this.LOG.setVerbose(verbose);

            // Use injected defaults if not specified via command line
            if (sizeThresholdMb == 0) {
                sizeThresholdMb = config.sizeThresholdMb();
            }
            if (!exportDatatables) {
                exportDatatables = config.exportDatatables();
            }
            if (plainTextMasks.isEmpty() && config.plainTextMasks().isPresent()) {
                plainTextMasks.addAll(Arrays.asList(config.plainTextMasks().get().split(",")));
            }
            if (exclusions.isEmpty() && config.exclusions().isPresent()) {
                exclusions.addAll(Arrays.asList(config.exclusions().get().split(",")));
            }

            RewriteConfig cfg = setupRewriteCfg();

            // runAsUsual(cfg);

            // Launch a separate Java JVM with its own classpaths packaging all the needed JARs
            runWithJavaProcess(cfg);

            // We stop to use an isolating approach with either a new URLClassLoader and/or ThreadExecutor and ComputableFuture, VirtualThreadcl
            // as they don't work !
            // runWithIsolatingApproach(cfg);
            // runWithVirtualThread(cfg);

        } catch (Exception e) {
            logger.error("Rewrite service failed !", e);
            System.exit(1);
        }
    }

    /**
     * Create the RewriteService, init it and runScanner
     * @param cfg the RewriteConfig
     */
    public void runAsUsual(RewriteConfig cfg) {
        RewriteService rewriteService;
        try {
            rewriteService = new RewriteService(cfg);
            rewriteService.init();
            ResultsContainer results = rewriteService.runScanner();
            rewriteService.showResults(results);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Launch a separate Java process to run RewriteService with an isolated classpath.
     * The classpath includes the additionalJars (resolved to full paths) and the
     * service-x-shaded.jar. The child process creates a RewriteService,
     * calls init() and runScanner() using the provided RewriteConfig.
     *
     * @param cfg the RewriteConfig
     */
    public void runWithJavaProcess(RewriteConfig cfg) {
        try (MavenArtifactResolver resolver = new MavenArtifactResolver()) {
            // Resolve additional jar paths (file paths or GAV coordinates) to absolute paths
            List<String> resolvedJarPaths = new ArrayList<>();
            if (additionalJarPaths != null && !additionalJarPaths.isEmpty()) {
                List<Path> resolved = resolver.resolveArtifacts(additionalJarPaths);
                for (Path p : resolved) {
                    resolvedJarPaths.add(p.toAbsolutePath().toString());
                }
            }

            // Resolve the shaded service jar using its GAV coordinate
            String shadedGav = "dev.snowdrop.openrewrite:service:jar:shaded:" + spec.version()[0];
            Path shadedJarPath = resolver.resolveArtifact(shadedGav);

            // Resolve the JBoss LogManager using also its GAV coodinate
            String logManagerGav = "org.jboss.logmanager:jboss-logmanager:3.2.1.Final";
            Path logManagerPath = resolver.resolveArtifact(logManagerGav);

            // Build the classpath: shaded jar + JBoss LogManager + additional jars
            List<String> classpathEntries = new ArrayList<>();
            classpathEntries.add(shadedJarPath.toAbsolutePath().toString());
            classpathEntries.add(logManagerPath.toAbsolutePath().toString());
            classpathEntries.addAll(resolvedJarPaths);
            String classpath = String.join(File.pathSeparator, classpathEntries);

            // Set the System property for the JBoss LogManager
            System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");

            // Build the java command
            String javaHome = System.getProperty("java.home");
            String javaBin = Path.of(javaHome, "bin", "java").toString();

            List<String> command = new ArrayList<>();
            command.add(javaBin);
            command.add("-cp");
            command.add(classpath);

            // Pass RewriteConfig fields as system properties to the child process
            command.add("-Drewrite.appPath=" + cfg.getAppPath().toAbsolutePath());

            if (cfg.getFqNameRecipe() != null) {
                command.add("-Drewrite.recipe=" + cfg.getFqNameRecipe());
            }
            if (cfg.getRecipeOptions() != null && !cfg.getRecipeOptions().isEmpty()) {
                command.add("-Drewrite.options=" + String.join(",", cfg.getRecipeOptions()));
            }
            if (cfg.getYamlRecipesPath() != null) {
                command.add("-Drewrite.yamlRecipesPath=" + cfg.getYamlRecipesPath());
            }
            if (!resolvedJarPaths.isEmpty()) {
                command.add("-Drewrite.additionalJarPaths=" + String.join(",", resolvedJarPaths));
            }
            command.add("-Drewrite.exportDatatables=" + cfg.canExportDatatables());
            command.add("-Drewrite.sizeThresholdMb=" + cfg.getSizeThresholdMb());
            command.add("-Drewrite.dryRun=" + cfg.isDryRun());
            command.add("-Drewrite.verbose=" + cfg.isVerbose());

            if (cfg.getExclusions() != null && !cfg.getExclusions().isEmpty()) {
                command.add("-Drewrite.exclusions=" + String.join(",", cfg.getExclusions()));
            }
            if (cfg.getPlainTextMasks() != null && !cfg.getPlainTextMasks().isEmpty()) {
                command.add("-Drewrite.plainTextMasks=" + String.join(",", cfg.getPlainTextMasks()));
            }

            // Main class entry point in the shaded jar
            command.add("dev.snowdrop.rewrite.service.RewriteServiceLauncher");

            logger.infof("Launching rewrite process with classpath: %s", classpath);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            pb.directory(cfg.getAppPath().toFile());

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Rewrite process exited with code: " + exitCode);
            }

            logger.info("Rewrite process completed successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Rewrite process was interrupted", e);
        } catch (Exception e) {
            logger.error("Failed to run rewrite process", e);
            throw new RuntimeException("Failed to run rewrite process", e);
        }
    }

    /**
     * Executes the rewrite process using the command-line options.
     *
     * @return the results container with recipe run results
     */
    public RewriteConfig setupRewriteCfg() {
        RewriteConfig cfg = new RewriteConfig();
        cfg.setAppPath(projectRoot.normalize().toAbsolutePath());
        cfg.setAdditionalJarPaths(additionalJarPaths);
        if (recipeName != null) {
            cfg.setFqNameRecipe(recipeName);
            cfg.setRecipeOptions(recipeOptions);
        }
        if (yamlRecipesPath != null) {
            cfg.setYamlRecipesPath(yamlRecipesPath);
        }
        cfg.setExportDatatables(exportDatatables);
        cfg.setExclusions(exclusions);
        cfg.setPlainTextMasks(plainTextMasks);
        cfg.setDryRun(dryRun);

        cfg.setVerbose(verbose);
        return cfg;
    }

    public void runWithVirtualThread(RewriteConfig cfg) {
        ClassLoader appLoader = ClassLoader.getSystemClassLoader();
        Log.debugf("System/Application ClassLoader: %s", appLoader);

        // Build classloader: AppClassLoader augmented with additional JARs when available
        ClassLoader rewriteClassLoader;
        if (additionalJarPaths != null && !additionalJarPaths.isEmpty()) {
            ClassLoaderUtils clu = new ClassLoaderUtils();
            rewriteClassLoader = clu.loadAdditionalJars(additionalJarPaths, appLoader);
        } else {
            rewriteClassLoader = appLoader;
        }

        CompletableFuture<ResultsContainer> future = new CompletableFuture<>();

        Thread vThread = Thread.ofVirtual()
                .name("rewrite-virtual")
                .unstarted(() -> {
                    Thread.currentThread().setContextClassLoader(rewriteClassLoader);
                    try {
                        Class<?> rewriteServiceClass = rewriteClassLoader
                                .loadClass("dev.snowdrop.rewrite.service.RewriteService");
                        Log.infof("RewriteService loaded via: %s", rewriteServiceClass.getClassLoader());

                        Object serviceInstance;
                        if (rewriteClassLoader instanceof URLClassLoader) {
                            serviceInstance = rewriteServiceClass
                                    .getDeclaredConstructor(RewriteConfig.class, URLClassLoader.class)
                                    .newInstance(cfg, (URLClassLoader) rewriteClassLoader);
                        } else {
                            serviceInstance = rewriteServiceClass
                                    .getDeclaredConstructor(RewriteConfig.class)
                                    .newInstance(cfg);
                        }

                        rewriteServiceClass.getMethod("init").invoke(serviceInstance);

                        Method runScannerMethod = rewriteServiceClass.getMethod("runScanner");
                        ResultsContainer results = (ResultsContainer) runScannerMethod.invoke(serviceInstance);

                        Method showResultsMethod = rewriteServiceClass.getMethod("showResults",ResultsContainer.class);
                        showResultsMethod.invoke(serviceInstance, results);

                        future.complete(results);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });

        vThread.start();

        try {
            future.get();
        } catch (ExecutionException | InterruptedException e) {
            Log.error("Virtual thread execution failed", e);
            throw new RuntimeException("Virtual thread execution failed", e);
        }
    }

    public void runWithIsolatingApproach(RewriteConfig cfg) {
        ClassLoader appLoader = ClassLoader.getSystemClassLoader();
        Log.debugf("System/Application ClassLoader: %s", appLoader);

        // Include the additional JARs provided as new URLClassloader
        if (!additionalJarPaths.isEmpty()) {
            URLClassLoader rewriteURLClassLoader = null;

            ClassLoaderUtils clu = new ClassLoaderUtils();
            rewriteURLClassLoader = clu.loadAdditionalJars(additionalJarPaths, appLoader);

                /*
                // The following code is not NEEDED when we create the client uber-jar as openrewrite artifavts are package
                // Add the Snowdrop OpenRewrite Service artifact packaging all the needed OpenRewrite JARs
                Log.info("Add the Snowdrop OpenRewrite Service artifact packaging all the needed OpenRewrite JARs to the list of the additional jars");
                additionalJarPaths.add("dev.snowdrop.openrewrite:service:jar:shaded:0.2.12-SNAPSHOT");

                URL[] newUrls = {
                  new File("/Users/cmoullia/.m2/repository/dev/snowdrop/openrewrite/service/0.2.12-SNAPSHOT/service-0.2.12-SNAPSHOT-shaded.jar").toURI().toURL(),
                  new File("/Users/cmoullia/.m2/repository/com/todo/app/0.0.1-SNAPSHOT/app-0.0.1-SNAPSHOT.jar").toURI().toURL(),
                  new File("/Users/cmoullia/.m2/repository/org/openrewrite/recipe/rewrite-java-dependencies/1.51.1/rewrite-java-dependencies-1.51.1.jar").toURI().toURL()
                };
                rewriteURLClassLoader = new URLClassLoader(newUrls,appLoader);
                Class<?> clazz = rewriteURLClassLoader.loadClass("org.openrewrite.java.JavaParser");
                logger.infof("Class found: %s in classloader: %s",clazz.getName(),clazz.getClassLoader());

                for (Enumeration<URL> e = rewriteURLClassLoader.getResources("META-INF/rewrite/classpath.tsv.gz"); e.hasMoreElements(); ) {
                    logger.infof("Resource found: %s",e.nextElement());
                }

                clu.checkClassLoaderResourcesFiles(this.getClass().getName(), rewriteURLClassLoader, "META-INF/rewrite/classpath.tsv.gz", "org/openrewrite/java/JavaParser.class");
                */

            // Use Executors.newSingleThreadExecutor and CompletableFuture => That fails too as the new Thread created 
            // don't use a new ClasLoader 
            // isolatedThreadClassLoader(rewriteURLClassLoader);

            // Use only the URLClassLoader
            usingUrlClassLoader(cfg, rewriteURLClassLoader);
        }
    }

    public void usingUrlClassLoader(RewriteConfig cfg, URLClassLoader rewriteURLClassLoader) {
        // We got an error as the OpenRewrite classes are not part of the AppClassLoader
        // rewriteService = new RewriteService(setupRewriteCfg(),rewriteURLClassLoader);
        Class<?> rewriteServiceClass = null;
        try {
            rewriteServiceClass = rewriteURLClassLoader.loadClass("dev.snowdrop.rewrite.service.RewriteService");
            Log.infof("RewriteService loaded via: %s", rewriteServiceClass.getClassLoader());
            
            // For debugging purposes !
            // searchResource(rewriteURLClassLoader,
            //         "org/openrewrite/java/JavaParser.class",
            //         "org/openrewrite/ExecutionContext.class",
            //         "org/openrewrite/config/ResourceLoader.class");

            /*
              rewriteURLClassLoader.loadClass("org.openrewrite.java.JavaParser");
              rewriteURLClassLoader.loadClass("org.openrewrite.ExecutionContext");
              rewriteURLClassLoader.loadClass("org.openrewrite.config.ResourceLoader");
              rewriteURLClassLoader.loadClass("org.openrewrite.LargeSourceSet");

              The classes that we are looking for ar well there within the URLClassLoader
               searchResource(rewriteURLClassLoader,
                   "org/openrewrite/java/JavaParser.class",
                   "org/openrewrite/ExecutionContext.class",
                   "org/openrewrite/config/ResourceLoader.class");

                BUT they are still loaded by the AppClassLoader => Quarkus

                Caused by: java.lang.ClassNotFoundException: org.openrewrite.ExecutionContext
                at java.base/jdk.internal.loader.BuiltinClassLoader.loadClass(BuiltinClassLoader.java:641)
                at java.base/jdk.internal.loader.ClassLoaders$AppClassLoader.loadClass(ClassLoaders.java:188)
                at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:526)
                ... 19 more
            */
            Object rewriteServiceInstance = rewriteServiceClass.getDeclaredConstructor(RewriteConfig.class, URLClassLoader.class).newInstance(cfg, rewriteURLClassLoader);

            Method initMethod = rewriteServiceClass.getMethod("init");
            initMethod.invoke(rewriteServiceInstance);

            Method runScannerMethod = rewriteServiceClass.getMethod("runScanner");
            ResultsContainer results = (ResultsContainer) runScannerMethod.invoke(rewriteServiceInstance);

            Method showResultsMethod = rewriteServiceClass.getMethod("showResults", ResultsContainer.class);
            showResultsMethod.invoke(rewriteServiceInstance, results);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                 InstantiationException e) {
            Log.error("Exception", e);
        }
    }

    public void isolatedThreadClassLoader(URLClassLoader rewriteURLClassLoader) {
        // Create a custom ExecutorService with the isolated classloader
        ExecutorService isolatedExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "rewrite-isolated");
            t.setContextClassLoader(rewriteURLClassLoader);
            return t;
        });

        CompletableFuture<ResultsContainer> future = CompletableFuture.supplyAsync(() -> {
            try {
                // Load RewriteService via the URLClassLoader — this is the key part
                Class<?> rewriteServiceClass = rewriteURLClassLoader
                        .loadClass("dev.snowdrop.rewrite.service.RewriteService");

                // RewriteConfig must come from the parent classloader (shared type)
                RewriteConfig cfg = setupRewriteCfg();

                Object serviceInstance = rewriteServiceClass
                        .getDeclaredConstructor(RewriteConfig.class, URLClassLoader.class)
                        .newInstance(cfg, rewriteURLClassLoader);

                // Call init()
                rewriteServiceClass.getMethod("init").invoke(serviceInstance);

                // Call runScanner()
                Method runScannerMethod = rewriteServiceClass.getMethod("runScanner");
                return (ResultsContainer) runScannerMethod.invoke(serviceInstance);
            } catch (Exception e) {
                throw new RuntimeException("Isolated rewrite execution failed", e);
            }
        }, isolatedExecutor);

        // Block and get results
        try {
            ResultsContainer results = future.get();
            Log.infof("Got results !!");
        } catch (ExecutionException | InterruptedException e) {
            Log.error("CompletableFuture execution failed", e);
        } finally {
            isolatedExecutor.shutdown();
        }
    }

    public void searchResource(URLClassLoader ucl, String... findNames) {
        for (String s : findNames) {
            Log.infof("Search about: %s in: %s", s, ucl.findResource(s));
        }
    }

    public void listClassLoaders() {
        Set<ClassLoader> loaders = Thread.getAllStackTraces().keySet().stream()
                .map(Thread::getContextClassLoader)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        loaders.forEach(cl -> Log.infof("ClassLoader active: %s.", cl));
    }

}