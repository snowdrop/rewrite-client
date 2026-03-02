package dev.snowdrop.openrewrite.cli.toolbox;

import org.jboss.logmanager.Logger;
import org.openrewrite.ExecutionContext;
import org.openrewrite.LargeSourceSet;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;

public class OpenRewriteLauncher {
    private static final Logger log = Logger.getLogger(OpenRewriteLauncher.class.getName());

    public OpenRewriteLauncher() {}

    /**
     * Entry-point invoked by {@code RewriteService.runRecipe} via reflection.
     *
     * @param recipe to be processed.
     * @return
     */
    public RecipeRun apply(Recipe recipe, LargeSourceSet sourceSet, ExecutionContext ctx) {
        log.info(String.format("Running openrewrite recipe: %s.%n",recipe.getName()));
        return recipe.run(sourceSet, ctx);
    }

}
