package dev.snowdrop.rewrite;

import dev.snowdrop.rewrite.toolbox.ClassLoaderUtils;
import org.jboss.logging.Logger;

import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RewriteServiceProxy {
    private final Logger logger = Logger.getLogger(RewriteServiceProxy.class);

    private URLClassLoader rewriteURLClassLoader;

    private String rewriteConfigClassName = "dev.snowdrop.rewrite.config.RewriteConfig";
    private String rewriteServiceClassName = "dev.snowdrop.rewrite.service.RewriteService";
    private String resultsContainerClassName = "dev.snowdrop.rewrite.ResultsContainer";
    private final String REWRITE_SERVICE_GAV = "dev.snowdrop.openrewrite:service:jar:shaded";
    private final String REWRITE_SERVICE_VERSION = "0.2.12-SNAPSHOT";

    private Path projectRoot;
    private List<String> additionalJarPaths;
    private String recipeName;
    private Set<String> recipeOptions;
    private String yamlRecipesPath;
    private boolean exportDatatables;
    private Set<String> exclusions;
    private Set<String> plainTextMasks;
    private boolean dryRun;
    private boolean verbose;

    public RewriteServiceProxy() {}

    public RewriteServiceProxy(URLClassLoader urlClassLoader) {
        rewriteURLClassLoader = urlClassLoader;
    }

    public void initClassLoader() {
        // Add the RewriteServiceGAV to the additional JARs and use it too to create the URLClassLoader
        // able to find the Rewrite client or OpenRewrite classes
        List<String> jars = new ArrayList<>(additionalJarPaths);
        jars.add(REWRITE_SERVICE_GAV + ":" + REWRITE_SERVICE_VERSION);

        ClassLoader appClassLoader = this.getClass().getClassLoader();
        ClassLoaderUtils clu = new ClassLoaderUtils();
        rewriteURLClassLoader = clu.loadAdditionalJars(jars, appClassLoader);
    }

    public Object runScanner() {
        try {
            Thread.currentThread().setContextClassLoader(rewriteURLClassLoader);
            Object cfg = setupRewriteCfg();

            // Load the RewriteService Class
            Class<?> rewriteServiceClass = rewriteURLClassLoader.loadClass(rewriteServiceClassName);
            logger.infof("Class found: %s in classloader: %s.", rewriteServiceClass.getName(), rewriteServiceClass.getClassLoader());

            // Load the RewriteConfig Class
            Class<?> configClass = rewriteURLClassLoader.loadClass(rewriteConfigClassName);
            logger.infof("Class found: %s in classloader: %s.", configClass.getName(), configClass.getClassLoader());

            // Instantiate the RewriteService using the constructor and pass as parameters: RewriteConfig and URLClassLoader
            Object rewriteServiceInstance = rewriteServiceClass.getDeclaredConstructor(configClass, URLClassLoader.class).newInstance(cfg, rewriteURLClassLoader);

            // Execute the init() method
            Method initMethod = rewriteServiceClass.getMethod("init");
            initMethod.invoke(rewriteServiceInstance);

            // Run the scanner()
            Method runScannerMethod = rewriteServiceClass.getMethod("runScanner");
            return runScannerMethod.invoke(rewriteServiceInstance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> findDataTableRows(Object resultsContainer, String recipeName, String dataTableName, String rowTypeName) {
        try {
            // Get the RecipeRun from ResultsContainer: results.getRecipeRuns().get(recipeName)
            Method getRecipeRunsMethod = resultsContainer.getClass().getMethod("getRecipeRuns");
            Map<?, ?> recipeRuns = (Map<?, ?>) getRecipeRunsMethod.invoke(resultsContainer);
            Object recipeRun = recipeRuns.get(recipeName);

            if (recipeRun == null) {
                throw new IllegalArgumentException("RecipeRun not found for recipe: " + recipeName);
            }

            // Load DataTableUtils and invoke findDataTableRows via reflection
            Class<?> dataTableUtilsClass = rewriteURLClassLoader.loadClass("dev.snowdrop.rewrite.utils.DataTableUtils");
            Class<?> recipeRunClass = rewriteURLClassLoader.loadClass("org.openrewrite.RecipeRun");
            Class<?> rowTypeClass = rewriteURLClassLoader.loadClass(rowTypeName);

            Method findMethod = dataTableUtilsClass.getMethod("findDataTableRows", recipeRunClass, String.class, Class.class);
            return (List<T>) findMethod.invoke(null, recipeRun, dataTableName, rowTypeClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void showResults(Object results) throws Exception {
        Class<?> rewriteServiceClass = rewriteURLClassLoader.loadClass(rewriteServiceClassName);
        Object rewriteServiceInstance = rewriteServiceClass.getDeclaredConstructor().newInstance();

        Class<?> resultscontainerClass = rewriteURLClassLoader.loadClass(resultsContainerClassName);

        invoke(rewriteServiceInstance,"showResults",resultscontainerClass,results);
    }

    public Object setupRewriteCfg() throws Exception {
        Class<?> cfgClass = rewriteURLClassLoader.loadClass(rewriteConfigClassName);
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

    public void setProjectRoot(Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    public void setAdditionalJarPaths(List<String> additionalJarPaths) {
        this.additionalJarPaths = additionalJarPaths;
    }

    public void setFQRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    public void setRecipeOptions(Set<String> recipeOptions) {
        this.recipeOptions = recipeOptions;
    }

    public void setYamlRecipesPath(String yamlRecipesPath) {
        this.yamlRecipesPath = yamlRecipesPath;
    }

    public void setExportDatatables(boolean exportDatatables) {
        this.exportDatatables = exportDatatables;
    }

    public void setExclusions(Set<String> exclusions) {
        this.exclusions = exclusions;
    }

    public void setPlainTextMasks(Set<String> plainTextMasks) {
        this.plainTextMasks = plainTextMasks;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
