package dev.snowdrop.openrewrite.cli.model;

import org.openrewrite.Recipe;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RewriteConfig {
    private Path appPath;

    // Additional jar containing recipes
    private List<String> additionalJarPaths = new ArrayList<>();

    // FQName of the recipe class to be executed
    private String namedRecipe;
    // Options to be set to the Recipe Class
    private Set<String> recipeOptions = new HashSet<>();

    // List of Recipe
    private List<Recipe> recipes = new ArrayList<>();

    // Path of the Yaml Recipes file
    private String yamlRecipesPath;

    private Boolean exportDatatables = true;
    private int sizeThresholdMb = 10;
    private Set<String> exclusions = new HashSet<>();
    private Set<String> plainTextMasks = new HashSet<>();
    private boolean dryRun = true;

    public Path getAppPath() {
        return appPath;
    }

    public void setAppPath(Path appPath) {
        this.appPath = appPath;
    }

    public List<String> getAdditionalJarPaths() {
        return additionalJarPaths;
    }

    public void setAdditionalJarPaths(List<String> additionalJarPaths) {
        this.additionalJarPaths = additionalJarPaths;
    }

    public String getNamedRecipe() {
        return namedRecipe;
    }

    public void setNamedRecipe(String namedRecipe) {
        this.namedRecipe = namedRecipe;
    }

    public List<Recipe> getRecipes(List<Recipe> recipes) {
        return recipes;
    }

    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes;
    }

    public String getYamlRecipesPath() {
        return yamlRecipesPath;
    }

    public void setYamlRecipesPath(String yamlRecipesPath) {
        this.yamlRecipesPath = yamlRecipesPath;
    }

    public Boolean canExportDatatables() {
        return exportDatatables;
    }

    public void setExportDatatables(Boolean exportDatatables) {
        this.exportDatatables = exportDatatables;
    }

    public int getSizeThresholdMb() {
        return sizeThresholdMb;
    }

    public void setSizeThresholdMb(int sizeThresholdMb) {
        this.sizeThresholdMb = sizeThresholdMb;
    }

    public Set<String> getExclusions() {
        return exclusions;
    }

    public void setExclusions(Set<String> exclusions) {
        this.exclusions = exclusions;
    }

    public Set<String> getPlainTextMasks() {
        return plainTextMasks;
    }

    public void setPlainTextMasks(Set<String> plainTextMasks) {
        this.plainTextMasks = plainTextMasks;
    }

    public Set<String> getRecipeOptions() {
        return recipeOptions;
    }

    public void setRecipeOptions(Set<String> recipeOptions) {
        this.recipeOptions = recipeOptions;
    }

    public Boolean getExportDatatables() {
        return exportDatatables;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
}
