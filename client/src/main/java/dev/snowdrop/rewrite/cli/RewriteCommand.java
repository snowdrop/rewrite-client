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
import dev.snowdrop.rewrite.toolbox.ClassLoaderUtils;
import io.quarkus.logging.Log;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import picocli.CommandLine;

import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
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

    String rewriteConfigClassName = "dev.snowdrop.rewrite.config.RewriteConfig";
    String rewriteServiceClassName = "dev.snowdrop.rewrite.service.RewriteService";
    String resultsContainerClassName = "dev.snowdrop.rewrite.ResultsContainer";
    String classLoaderUtilClassName = "dev.snowdrop.rewrite.toolbox.ClassLoaderUtils";

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

            // Include the shaded jar of OpenRewrite and Rewrite services
            additionalJarPaths.add("dev.snowdrop.openrewrite:service:jar:shaded:" + spec.version()[0]);

            /*
             List<String> paths = List.of(
                    "/Users/cmoullia/.m2/repository/dev/snowdrop/openrewrite/service/0.2.12-SNAPSHOT/service-0.2.12-SNAPSHOT-shaded.jar",
                    "/Users/cmoullia/.m2/repository/org/openrewrite/recipe/rewrite-java-dependencies/1.51.1/rewrite-java-dependencies-1.51.1.jar",
                    "/Users/cmoullia/.m2/repository/com/todo/app/0.0.1-SNAPSHOT/app-0.0.1-SNAPSHOT.jar"
             );

             URL[] urlArray = paths.stream()
                    .map(File::new)
                    .map(f -> {
                        try {
                            return f.toURI().toURL();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toArray(URL[]::new);

              rewriteURLClassLoader = new URLClassLoader(urlArray, appClassLoader);
             */

            // Create the UrlClassLoader
            ClassLoader appClassLoader = this.getClass().getClassLoader();
            ClassLoaderUtils clu = new ClassLoaderUtils();
            URLClassLoader rewriteURLClassLoader = clu.loadAdditionalJars(additionalJarPaths, appClassLoader);

            Thread.currentThread().setContextClassLoader(rewriteURLClassLoader);

            // Set up the RewriteConfiguration
            Object cfg = setupRewriteCfg(rewriteURLClassLoader);

            Class<?> rewriteServiceClass = null;

            rewriteServiceClass = rewriteURLClassLoader.loadClass(rewriteServiceClassName);
            logger.infof("Class found: %s in classloader: %s. %n", rewriteServiceClass.getName(), rewriteServiceClass.getClassLoader());

            Class<?> configClass = rewriteURLClassLoader.loadClass(rewriteConfigClassName);
            logger.infof("Class found: %s in classloader: %s. %n", configClass.getName(), configClass.getClassLoader());

            Object rewriteServiceInstance = rewriteServiceClass.getDeclaredConstructor(configClass, URLClassLoader.class).newInstance(cfg, rewriteURLClassLoader);


            Method initMethod = rewriteServiceClass.getMethod("init");
            initMethod.invoke(rewriteServiceInstance);

            Method runScannerMethod = rewriteServiceClass.getMethod("runScanner");
            Class<?> resultsContainerClazz = rewriteURLClassLoader.loadClass(resultsContainerClassName);
            Object resultsContainer = runScannerMethod.invoke(rewriteServiceInstance);

            Method showResultsMethod = rewriteServiceClass.getMethod("showResults",resultsContainerClazz);
            showResultsMethod.invoke(rewriteServiceInstance,resultsContainer);

        } catch (Exception e) {
            logger.error("Rewrite service failed !", e);
            System.exit(1);
        }
    }

    public Object setupRewriteCfg(ClassLoader urlClassLoader) throws Exception {

        Class<?> cfgClass = urlClassLoader.loadClass(rewriteConfigClassName);
        Object cfg = cfgClass.getDeclaredConstructor().newInstance();

        invoke(cfg, "setAppPath", Path.class, projectRoot.normalize().toAbsolutePath());
        invoke(cfg, "setAdditionalJarPaths", List.class, additionalJarPaths);

        if (recipeName != null) {
            invoke(cfg, "setFqNameRecipe", String.class, recipeName);
            invoke(cfg, "setRecipeOptions", Set.class, recipeOptions);
        }

        if (yamlRecipesPath != null) {
            invoke(cfg, "setYamlRecipesPath", String.class, yamlRecipesPath);
        }

        invoke(cfg, "setExportDatatables", Boolean.class, exportDatatables);
        invoke(cfg, "setExclusions", Set.class, exclusions);
        invoke(cfg, "setPlainTextMasks", Set.class, plainTextMasks);
        invoke(cfg, "setDryRun", boolean.class, dryRun);
        invoke(cfg, "setVerbose", boolean.class, verbose);

        return cfg;
    }

    /**
     * Utility method to reduce reflection boilerplate
     *
     * @param target     The target class
     * @param methodName The method name to be invoked on the target class
     * @param paramType  The type of the parameter
     * @param value      The value of the parameter
     * @throws Exception
     */
    private void invoke(Object target, String methodName, Class<?> paramType, Object value) throws Exception {
        Method method = target.getClass().getMethod(methodName, paramType);
        method.invoke(target, value);
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