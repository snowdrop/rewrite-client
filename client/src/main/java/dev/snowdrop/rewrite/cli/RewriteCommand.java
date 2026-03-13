/// usr/bin/env jbang “$0” “$@” ; exit $?
//DEPS dev.snowdrop.openrewrite:rewrite-client:0.3.1
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
import dev.snowdrop.rewrite.cli.logging.LoggerUtils;
import dev.snowdrop.rewrite.cli.logging.LoggingConfiguration;
import dev.snowdrop.rewrite.config.RewriteConfig;
import dev.snowdrop.rewrite.service.RewriteService;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import io.quarkus.runtime.LaunchMode;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;

import org.jboss.logging.Logger;

/**
 * Quarkus-based standalone CLI for OpenRewrite
 */
@TopCommand
@CommandLine.Command(
        name = "rewrite",
        mixinStandardHelpOptions = true,
        version = "0.3.1",
        description = "Standalone OpenRewrite CLI tool for applying recipe on the code source of an application"
)
public class RewriteCommand implements Runnable {
    private final Logger logger = Logger.getLogger(RewriteCommand.class.getName());

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
            names = {"-v"},
            description = "Enable more tracing output: DEBUG, TRACE using -v, -vv respectively")
    boolean[] verbosity = new boolean[0];

    @Inject
    RewriteConfiguration config;

    @Inject
    LoggerUtils loggerUtils;

    @Inject
    LoggingConfiguration loggingConfig;

    @Inject
    LaunchMode mode;

    /**
     * Creates a new RewriteCommand instance.
     */
    public RewriteCommand() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        if (mode.isProduction()) {
            loggerUtils.setupLogManagerAndHandler(loggingConfig, verbosity.length, spec);
        }

        try {
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

            // Use RewriteService class
            logger.info("Launching the rewrite service ...");
            RewriteService rewriteService;
            try {
                rewriteService = new RewriteService(cfg);
                rewriteService.init();
                ResultsContainer results = rewriteService.runScanner();
                rewriteService.showResults(results);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } catch (Exception e) {
            logger.error("Rewrite service failed !", e);
            System.exit(1);
        }
    }

    /**
     * Use the client parameters to create the RewriteConfig
     *
     * @return the RewriteConfig
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
        return cfg;
    }
}