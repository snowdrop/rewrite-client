package dev.snowdrop.openrewrite.cli.toolbox;

import dev.snowdrop.openrewrite.cli.model.ResultsContainer;
import dev.snowdrop.openrewrite.cli.model.RewriteConfig;
import org.jboss.logmanager.Logger;
import org.openrewrite.*;
import org.openrewrite.config.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static dev.snowdrop.openrewrite.cli.RewriteService.configureRecipeOptions;

public class OpenRewriteLauncher {
    private static final Logger log = Logger.getLogger(OpenRewriteLauncher.class.getName());

    private ExecutionContext ctx;
    private Environment env;
    private LargeSourceSet sourceSet;
    private RewriteConfig rewriteConfig;
    private List<String> yamlDefinedRecipeNames;
    private URLClassLoader additionalJarsClassloader;

    public OpenRewriteLauncher() {}

    /**
     * Initialises the launcher with all objects needed to process recipes.
     * Called via reflection from {@code RewriteService.run()} so that the merged
     * classloader context is used for all subsequent operations.
     * <p>
     * The {@link Environment} is created here (rather than in {@code RewriteService})
     * so that {@code env.load()} uses the merged classloader as the first resource
     * loader instead of the QuarkusApp classloader.
     *
     * @param ctx                       the execution context
     * @param additionalJarsClassloader optional classloader for additional recipe JARs (may be null)
     * @param sourceSet                 the parsed source files
     * @param rewriteConfig             the rewrite configuration
     * @param yamlDefinedRecipeNames    recipe names discovered from the YAML file (populated here)
     */
    public void init(ExecutionContext ctx, URLClassLoader additionalJarsClassloader, LargeSourceSet sourceSet,
                     RewriteConfig rewriteConfig, List<String> yamlDefinedRecipeNames) throws Exception {
        this.ctx = ctx;
        this.additionalJarsClassloader = additionalJarsClassloader;
        this.sourceSet = sourceSet;
        this.rewriteConfig = rewriteConfig;
        this.yamlDefinedRecipeNames = yamlDefinedRecipeNames;
        log.info("OpenRewriteLauncher initialised. Classloader: " + getClass().getClassLoader());

        // Build the environment here so that env.load() uses the merged classloader
        this.env = createEnvironment();
    }

    /**
     * Activates, validates and runs the configured recipes.
     * Replaces {@code RewriteService.processRecipes()} and is called via reflection
     * from {@code RewriteService.run()}.
     *
     * @return the results container with all recipe run results
     */
    public ResultsContainer apply() {
        ClassLoader mergedClassLoader = additionalJarsClassloader != null
                ? additionalJarsClassloader
                : getClass().getClassLoader();

        if (ctx == null || env == null || sourceSet == null) {
            throw new IllegalStateException("OpenRewriteLauncher.init() must be called before apply()");
        }

        if (env.listRecipes().isEmpty()) {
            log.warning(String.format("No recipes found in active selection or YAML configuration for path: %s",
                rewriteConfig.getAppPath()));
            return new ResultsContainer(Collections.emptyMap());
        }

        Map<String, RecipeRun> allResults = new HashMap<>();
        boolean yamlRecipes = rewriteConfig.getYamlRecipesPath() != null
            && !rewriteConfig.getYamlRecipesPath().isEmpty();

        if (!yamlRecipes) {
            // FQName-based recipe
            if (rewriteConfig.getFqNameRecipe() == null || rewriteConfig.getFqNameRecipe().isEmpty()) {
                log.warning("No recipe FQName configured.");
                return new ResultsContainer(Collections.emptyMap());
            }

            Thread.currentThread().setContextClassLoader(mergedClassLoader);
            Recipe recipe = env.activateRecipes(rewriteConfig.getFqNameRecipe());

            if (rewriteConfig.getRecipeOptions() != null && !rewriteConfig.getRecipeOptions().isEmpty()) {
                configureRecipeOptions(recipe, rewriteConfig.getRecipeOptions());
            }

            log.info("Using active recipe(s): " + recipe.getName());

            if ("org.openrewrite.Recipe$Noop".equals(recipe.getName())) {
                log.severe("No recipes were activated. Activate a recipe by providing it as a command line argument.");
                return new ResultsContainer(Collections.emptyMap());
            }

            validatingRecipe(recipe);
            RecipeRun recipeRun = runRecipe(recipe);
            allResults.put(recipe.getName(), recipeRun);

        } else {
            // YAML-defined recipes
            log.info("Using recipes from YAML configuration");
            Recipe yamlRecipe = env.activateRecipes(yamlDefinedRecipeNames.toArray(new String[0]));
            log.info("Running recipe: " + yamlRecipe.getName());
            validatingRecipe(yamlRecipe);
            RecipeRun currentRun = runRecipe(yamlRecipe);
            allResults.put(yamlRecipe.getName(), currentRun);
        }

        return new ResultsContainer(allResults);
    }

    /**
     * Creates the OpenRewrite {@link Environment} with recipe loaders.
     * The {@link Environment.Builder} is constructed with the merged classloader set as the
     * thread context classloader so that {@code scanRuntimeClasspath()} and {@code env.load()}
     * do not pick up the QuarkusApp classloader as the first resource loader.
     */
    private Environment createEnvironment() throws Exception {
        ClassLoader mergedClassLoader = additionalJarsClassloader != null
            ? additionalJarsClassloader
            : getClass().getClassLoader();

        verifyClasspathResource(mergedClassLoader);

        // Temporarily set the merged classloader as the thread context classloader so that
        // Environment.builder() / scanRuntimeClasspath() / load() use it instead of the
        // QuarkusApp classloader that is normally set on the thread.
        ClassLoader previousContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(mergedClassLoader);

        try {
            Environment.Builder envBuilder = Environment.builder();

            // Scan the runtime classpath using the merged classloader explicitly,
            // instead of scanRuntimeClasspath() which relies on the thread context classloader.
            envBuilder.load(new ClasspathScanningLoader(new Properties(), mergedClassLoader));
            log.info("Loaded recipes from merged classloader: " + mergedClassLoader);

            /* Not needed as the additional jars are part of the mergedClassLoader
            if (additionalJarsClassloader != null) {
                // Also load recipes from the additional JARs classloader if distinct
                envBuilder.load(new ClasspathScanningLoader(new Properties(), additionalJarsClassloader));
                log.info("Loaded recipes from additional JARs classloader");
            }
            */

            // Load YAML recipes if configured, while the builder is still open
            if (rewriteConfig.getYamlRecipesPath() != null && !rewriteConfig.getYamlRecipesPath().isEmpty()) {
                ClassLoader yamlClassLoader = additionalJarsClassloader != null
                    ? additionalJarsClassloader
                    : mergedClassLoader;
                loadRecipesFromYAML(envBuilder, yamlClassLoader);
            }

            return envBuilder.build();
        } finally {
            Thread.currentThread().setContextClassLoader(previousContextClassLoader);
        }
    }

    /**
     * Verifies whether the given classloader can locate the OpenRewrite type-mapping resource
     * {@code META-INF/rewrite/classpath.tsv.gz}. Logs the result at INFO level.
     *
     * @param classLoader the classloader to inspect
     */
    private void verifyClasspathResource(ClassLoader classLoader) {
        java.net.URL resource = classLoader.getResource("META-INF/rewrite/classpath.tsv.gz");
        if (resource != null) {
            log.info("META-INF/rewrite/classpath.tsv.gz found in merged classloader: " + resource);
        } else {
            log.warning("META-INF/rewrite/classpath.tsv.gz NOT found in merged classloader: " + classLoader);
        }
    }

    private void loadRecipesFromYAML(Environment.Builder envBuilder, ClassLoader yamlClassLoader) throws Exception {
        Path configPath;
        if (Paths.get(rewriteConfig.getYamlRecipesPath()).isAbsolute()) {
            configPath = Paths.get(rewriteConfig.getYamlRecipesPath());
        } else {
            String appProject = System.getenv("APP_PROJECT");
            if (appProject != null && !appProject.isEmpty()) {
                configPath = Paths.get(appProject);
            } else {
                // Fall back to resolving against current working directory + YAML path
                configPath = rewriteConfig.getAppPath().resolve(rewriteConfig.getYamlRecipesPath());
            }
        }

        if (Files.exists(configPath)) {
            try (InputStream is = Files.newInputStream(configPath)) {
                var yamlRecipesPath = configPath.normalize().toUri();
                YamlResourceLoader loader = new YamlResourceLoader(is, yamlRecipesPath, new Properties(), yamlClassLoader);
                loader.listRecipes().forEach(rd -> yamlDefinedRecipeNames.add(rd.getName()));
                envBuilder.load(loader);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Runs a single recipe directly (no additional reflection hop needed here since
     * we are already inside the merged classloader).
     */
    private RecipeRun runRecipe(Recipe recipe) {
        log.info(String.format("Running openrewrite recipe: %s.%n", recipe.getName()));
        RecipeRun rr = recipe.run(sourceSet, ctx);

        if (rewriteConfig.canExportDatatables()) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            String timestamp = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS"));
            java.nio.file.Path datatableDirectoryPath = rewriteConfig.getAppPath()
                .resolve("target").resolve("rewrite").resolve("datatables").resolve(timestamp);
            log.info("Printing available datatables to: " + datatableDirectoryPath);
            rr.exportDatatablesToCsv(datatableDirectoryPath, ctx);
        }

        return rr;
    }

    private void validatingRecipe(Recipe recipe) {
        log.info("Validating active recipes...");
        List<Validated<Object>> validations = new ArrayList<>();
        recipe.validateAll(ctx, validations);
        List<Validated.Invalid<Object>> failedValidations = validations.stream()
            .map(Validated::failures)
            .flatMap(java.util.Collection::stream)
            .toList();

        if (!failedValidations.isEmpty()) {
            failedValidations.forEach(fv ->
                log.severe("Recipe validation error in " + fv.getProperty() + ": " + fv.getMessage()));
            log.warning("Recipe validation errors detected. Execution will continue regardless.");
        }
    }
}