package dev.snowdrop.rewrite.service;

import dev.snowdrop.rewrite.ResultsContainer;
import dev.snowdrop.rewrite.config.RewriteConfig;

import java.nio.file.Path;
import java.util.*;

/**
 * Entry point for running RewriteService in a separate Java process.
 * Reads RewriteConfig from system properties passed by the parent process.
 */
public class RewriteServiceLauncher {

    public static void main(String[] args) {
        RewriteConfig cfg = buildConfigFromSystemProperties();

        try {
            RewriteService rewriteService = new RewriteService(cfg);
            rewriteService.init();
            ResultsContainer results = rewriteService.runScanner();
            rewriteService.showResults(results);
        } catch (Exception e) {
            System.err.println("Rewrite service failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static RewriteConfig buildConfigFromSystemProperties() {
        RewriteConfig cfg = new RewriteConfig();

        String appPath = System.getProperty("rewrite.appPath");
        if (appPath != null) {
            cfg.setAppPath(Path.of(appPath));
        }

        String recipe = System.getProperty("rewrite.recipe");
        if (recipe != null) {
            cfg.setFqNameRecipe(recipe);
        }

        String options = System.getProperty("rewrite.options");
        if (options != null && !options.isEmpty()) {
            cfg.setRecipeOptions(new LinkedHashSet<>(Arrays.asList(options.split(","))));
        }

        String yamlRecipesPath = System.getProperty("rewrite.yamlRecipesPath");
        if (yamlRecipesPath != null) {
            cfg.setYamlRecipesPath(yamlRecipesPath);
        }

        String additionalJars = System.getProperty("rewrite.additionalJarPaths");
        if (additionalJars != null && !additionalJars.isEmpty()) {
            cfg.setAdditionalJarPaths(new ArrayList<>(Arrays.asList(additionalJars.split(","))));
        }

        String exclusions = System.getProperty("rewrite.exclusions");
        if (exclusions != null && !exclusions.isEmpty()) {
            cfg.setExclusions(new HashSet<>(Arrays.asList(exclusions.split(","))));
        }

        String plainTextMasks = System.getProperty("rewrite.plainTextMasks");
        if (plainTextMasks != null && !plainTextMasks.isEmpty()) {
            cfg.setPlainTextMasks(new HashSet<>(Arrays.asList(plainTextMasks.split(","))));
        }

        cfg.setExportDatatables(Boolean.parseBoolean(System.getProperty("rewrite.exportDatatables", "true")));
        cfg.setSizeThresholdMb(Integer.parseInt(System.getProperty("rewrite.sizeThresholdMb", "10")));
        cfg.setDryRun(Boolean.parseBoolean(System.getProperty("rewrite.dryRun", "true")));
        cfg.setVerbose(Boolean.parseBoolean(System.getProperty("rewrite.verbose", "false")));

        return cfg;
    }
}