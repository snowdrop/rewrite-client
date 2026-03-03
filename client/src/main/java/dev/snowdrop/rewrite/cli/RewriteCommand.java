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

import dev.snowdrop.rewrite.cli.toolbox.LoggerUtils;
import dev.snowdrop.rewrite.config.RewriteConfig;
//import dev.snowdrop.openrewrite.cli.toolbox.ClassLoaderUtils;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import picocli.CommandLine;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

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

            List<String> rewriteJars = List.of(
                    "org.openrewrite:rewrite-core:8.70.4",
                    "org.openrewrite:rewrite-java:8.70.4");

            List<String> rewriteJarPaths = List.of(
                    "/Users/cmoullia/.m2/repository/dev/snowdrop/openrewrite/service/0.2.12-SNAPSHOT/service-0.2.12-SNAPSHOT-runner.jar",
                    "/Users/cmoullia/.m2/repository/org/openrewrite/rewrite-core/8.70.4/rewrite-core-8.70.4.jar",
                    "/Users/cmoullia/.m2/repository/org/openrewrite/rewrite-java/8.70.4/rewrite-java-8.70.4.jar");

            List<URL> urls = new ArrayList<>();
            for (String path : rewriteJarPaths) {
                File jar = new File(path);
                if (!jar.exists()) {
                    System.err.printf("JAR not found, skipping: %s%n", path);
                    continue;
                }
                urls.add(jar.toURI().toURL());
                System.out.printf("  + %s%n", jar.getAbsolutePath());
            }

            if (urls.isEmpty()) {
                System.err.println("No valid JARs found. Aborting.");
                return;
            }

            ClassLoader appClassLoader = getClass().getClassLoader();
            URLClassLoader mergedLoader = new URLClassLoader(
                    urls.toArray(new URL[0]),
                    appClassLoader   // add the running app's classloader
            );


            Class<?> rewriteServiceClass = mergedLoader.loadClass("dev.snowdrop.rewrite.service.RewriteService");
            System.out.printf("Service loaded via: %s%n", rewriteServiceClass.getClassLoader());

            Object rewriteServiceInstance = rewriteServiceClass.getDeclaredConstructor(RewriteConfig.class).newInstance(setupRewriteCfg());
            Method runScannerMethod = rewriteServiceClass.getMethod("runScanner");
            runScannerMethod.invoke(rewriteServiceInstance);

            System.out.println("Launcher finished successfully.");

            /* Old code
            // Create the RewriteConfig using the command parameters, options
            RewriteService rewriteService = new RewriteService(setupRewriteCfg());
            rewriteService.setLogger(this.LOG);
            ResultsContainer results = rewriteService.runScanner();
            rewriteService.showResults(results);
             */

            /*
            ERROR [dev.snowdrop.openrewrite.cli.RewriteCommand] Error executing rewrite command:
            java.lang.ClassCastException: class jdk.internal.loader.ClassLoaders$AppClassLoader cannot be cast to class dev.snowdrop.openrewrite.cli.toolbox.RewriteClassLoader
            (jdk.internal.loader.ClassLoaders$AppClassLoader is in module java.base of loader 'bootstrap'; dev.snowdrop.openrewrite.cli.toolbox.RewriteClassLoader is in unnamed module of loader 'app')

            RewriteClassLoader rcl = (RewriteClassLoader) getClass().getClassLoader();
            ClassLoaderUtils clu = new ClassLoaderUtils();
            clu.resolveAdditionalJarPaths(rewriteJars).forEach(p -> {
                try {
                    rcl.addURL(URI.create(p.toString()).toURL());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            });

            Thread.currentThread().setContextClassLoader(rcl);
            */

            /* To check if the classes have been well loaded
               Class c = rcl.loadClass("org.openrewrite.InMemoryExecutionContext");
               c = rcl.loadClass("org.openrewrite.internal.InMemoryLargeSourceSet");
               c = rcl.loadClass("org.openrewrite.ExecutionContext");
               c = rcl.loadClass("org.openrewrite.LargeSourceSet");

               ClassLoaderUtils clu = new ClassLoaderUtils();
               ClassLoader appClassloader = getClass().getClassLoader();
               URLClassLoader rcl = clu.loadAdditionalJars(rewriteJars,appClassloader);
             */

        } catch (Exception e) {
            //LOG.error(RewriteCommand.class, "Error executing rewrite command", e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Executes the rewrite process using the command-line options.
     *
     * @return the results container with recipe run results
     * @throws Exception if execution fails
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
}