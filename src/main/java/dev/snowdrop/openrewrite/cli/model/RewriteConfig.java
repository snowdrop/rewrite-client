package dev.snowdrop.openrewrite.cli.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration model holding all settings for an OpenRewrite execution.
 */
public class RewriteConfig {

    /** Creates a new RewriteConfig with default settings. */
    public RewriteConfig() {
    }

    private Path appPath;

    // Additional jar containing recipes
    private List<String> additionalJarPaths = new ArrayList<>();

    // FQName of the recipe class to be executed
    private String fqNameRecipe;

    // List of string defined as k=v, k=v, etc to configure the fields of the Recipe class
    private Set<String> recipeOptions = new HashSet<>();

    // Path of the Yaml Recipes file
    private String yamlRecipesPath;

    private Boolean exportDatatables = true;
    private int sizeThresholdMb = 10;
    private Set<String> exclusions = new HashSet<>();
    private Set<String> plainTextMasks = new HashSet<>();
    private boolean dryRun = true;

    /**
     * Returns the application project path.
     *
     * @return the application path
     */
    public Path getAppPath() {
        return appPath;
    }

    /**
     * Sets the application project path.
     *
     * @param appPath the application path
     */
    public void setAppPath(Path appPath) {
        this.appPath = appPath;
    }

    /**
     * Returns the list of additional JAR paths or Maven GAV coordinates.
     *
     * @return the additional JAR paths
     */
    public List<String> getAdditionalJarPaths() {
        return additionalJarPaths;
    }

    /**
     * Sets the list of additional JAR paths or Maven GAV coordinates.
     *
     * @param additionalJarPaths the additional JAR paths
     */
    public void setAdditionalJarPaths(List<String> additionalJarPaths) {
        this.additionalJarPaths = additionalJarPaths;
    }

    /**
     * Returns the fully qualified name of the recipe class.
     *
     * @return the recipe class name
     */
    public String getFqNameRecipe() {
        return fqNameRecipe;
    }

    /**
     * Sets the fully qualified name of the recipe class.
     *
     * @param fqNameRecipe the recipe class name
     */
    public void setFqNameRecipe(String fqNameRecipe) {
        this.fqNameRecipe = fqNameRecipe;
    }

    /**
     * Returns the path to the YAML recipes file.
     *
     * @return the YAML recipes path
     */
    public String getYamlRecipesPath() {
        return yamlRecipesPath;
    }

    /**
     * Sets the path to the YAML recipes file.
     *
     * @param yamlRecipesPath the YAML recipes path
     */
    public void setYamlRecipesPath(String yamlRecipesPath) {
        this.yamlRecipesPath = yamlRecipesPath;
    }

    /**
     * Returns whether datatables can be exported.
     *
     * @return true if datatables export is enabled
     */
    public Boolean canExportDatatables() {
        return exportDatatables;
    }

    /**
     * Sets whether to export datatables.
     *
     * @param exportDatatables true to enable datatables export
     */
    public void setExportDatatables(Boolean exportDatatables) {
        this.exportDatatables = exportDatatables;
    }

    /**
     * Returns the size threshold in MB.
     *
     * @return the size threshold
     */
    public int getSizeThresholdMb() {
        return sizeThresholdMb;
    }

    /**
     * Sets the size threshold in MB.
     *
     * @param sizeThresholdMb the size threshold
     */
    public void setSizeThresholdMb(int sizeThresholdMb) {
        this.sizeThresholdMb = sizeThresholdMb;
    }

    /**
     * Returns the set of file exclusion patterns.
     *
     * @return the exclusion patterns
     */
    public Set<String> getExclusions() {
        return exclusions;
    }

    /**
     * Sets the file exclusion patterns.
     *
     * @param exclusions the exclusion patterns
     */
    public void setExclusions(Set<String> exclusions) {
        this.exclusions = exclusions;
    }

    /**
     * Returns the set of plain text file masks.
     *
     * @return the plain text masks
     */
    public Set<String> getPlainTextMasks() {
        return plainTextMasks;
    }

    /**
     * Sets the plain text file masks.
     *
     * @param plainTextMasks the plain text masks
     */
    public void setPlainTextMasks(Set<String> plainTextMasks) {
        this.plainTextMasks = plainTextMasks;
    }

    /**
     * Returns the set of recipe options.
     *
     * @return the recipe options
     */
    public Set<String> getRecipeOptions() {
        return recipeOptions;
    }

    /**
     * Sets the recipe options.
     *
     * @param recipeOptions the recipe options
     */
    public void setRecipeOptions(Set<String> recipeOptions) {
        this.recipeOptions = recipeOptions;
    }

    /**
     * Returns the export datatables flag.
     *
     * @return true if datatables export is enabled
     */
    public Boolean getExportDatatables() {
        return exportDatatables;
    }

    /**
     * Returns whether dry run mode is enabled.
     *
     * @return true if dry run mode is enabled
     */
    public boolean isDryRun() {
        return dryRun;
    }

    /**
     * Sets the dry run mode.
     *
     * @param dryRun true to enable dry run mode
     */
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
}