///usr/bin/env jbang “$0” “$@” ; exit $?
//DEPS dev.snowdrop.openrewrite:rewrite-client:1.0.0-SNAPSHOT
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

// List of DEPS generated using the command: mvn dependency:list -DexcludeTransitive=true | grep ":.*:.*:.*" | cut -d']' -f2- | sed 's/^ /\/\/DEPS /'

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
package dev.snowdrop.openrewrite.cli;

import dev.snowdrop.openrewrite.cli.model.RewriteConfig;
import dev.snowdrop.openrewrite.cli.model.ResultsContainer;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.inject.Inject;
import org.openrewrite.DataTable;
import org.openrewrite.RecipeRun;
import org.openrewrite.table.SearchResults;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;

/**
 * Quarkus-based standalone CLI for OpenRewrite
 */
@TopCommand
@CommandLine.Command(
    name = "rewrite",
    mixinStandardHelpOptions = true,
    version = "1.0.0-SNAPSHOT",
    description = "Standalone OpenRewrite CLI tool for applying recipe on the code source of an application",
    footer = "\nExample usage:\n" +
        "  rewrite /path/to/project org.openrewrite.java.format.AutoFormat\n" +
        "  rewrite --jar custom-recipes.jar --export-datatables /path/to/project MyRecipe\n" +
        "  rewrite --jar org.openrewrite:rewrite-java:8.62.4,dev.snowdrop:openrewrite-recipes:1.0.0-SNAPSHOT /path/to/project MyRecipe\n" +
        "  rewrite --config /path/to/rewrite.yml /path/to/project MyRecipe"
)
public class RewriteCommand implements Runnable {

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
        names= {"-o","--options"},
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
        names = {"-d","--dry-run"},
        description = "Execute the recipes in dry run mode"
    )
    boolean dryRun = true;

    /*
     The @Inject annotation is disabled and replaced with ConfigProvider.getConfig()
     as we got an Arc error during the execution of the jbang export command
     Unsatisfied dependency for type dev.snowdrop.openrewrite.cli.RewriteConfiguration and qualifiers [@Default]
     */
    @Inject
    RewriteConfiguration config;

    @Override
    public void run() {
        try {
            //config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class).getConfigMapping(RewriteConfiguration.class);

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

            ResultsContainer results = execute();
            Map<String, RecipeRun> runs = results.getRecipeRuns();

            runs.forEach((k,v) -> {
                if (!v.getDataTables().isEmpty()) {
                    System.out.printf("Execution of the recipe %s succeeded\n",k);
                    // The DataTable<SearchResult> will be available starting from: 8.69.0 !

                    Map<DataTable<?>, List<?>> searchResults = v.getDataTables();
                    if (searchResults != null) {
                        searchResults.forEach((result, list) -> {
                            if (result.getClass().getSimpleName().startsWith("SearchResults")) {
                                System.out.println("# Found " + list.size() + " search results.");
                                list.stream().forEach(r -> {
                                    var row = (SearchResults.Row)r;
                                    System.out.println("# SourcePath: " + row.getSourcePath());
                                    System.out.println("# Result: " + row.getResult());
                                    System.out.println("# Recipe: " + row.getRecipe());
                                    System.out.println("==============================================");
                                });
                            }
                        });
                    }
                }
            });
            System.out.println("Finished OpenRewrite ...");

        } catch (Exception e) {
            System.err.println("Error executing rewrite command: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public ResultsContainer execute(RewriteConfig rewriteConfig) throws Exception {
        return runScanner(rewriteConfig);
    }

    public ResultsContainer execute() throws Exception {
        RewriteConfig cfg = new RewriteConfig();
        cfg.setAppPath(projectRoot);
        cfg.setAdditionalJarPaths(additionalJarPaths);
        if (recipeName != null) {
            cfg.setFqNameRecipe(recipeName);
            cfg.setRecipeOptions(recipeOptions);
        }
        if (yamlRecipesPath != null) {cfg.setYamlRecipesPath(yamlRecipesPath);}
        cfg.setExportDatatables(exportDatatables);
        cfg.setExclusions(exclusions);
        cfg.setPlainTextMasks(plainTextMasks);
        cfg.setDryRun(dryRun);

        return runScanner(cfg);
    }

    private ResultsContainer runScanner(RewriteConfig cfg) throws Exception {
        System.out.println("Starting OpenRewrite ...");
        System.out.println("Project root: " + cfg.getAppPath().toAbsolutePath());
        System.out.println("Fully Qualified named of the Recipe java class: " + cfg.getFqNameRecipe());

        if (!cfg.getAdditionalJarPaths().isEmpty()) {
            System.out.println("Additional JAR files: " + cfg.getAdditionalJarPaths());
        }

        RewriteService scanner = new RewriteService(cfg);
        scanner.init();
        return scanner.run();
    }
}