///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.jboss.slf4j:slf4j-jboss-logmanager:2.1.0.Final
//DEPS dev.snowdrop.openrewrite:service:0.3.4-SNAPSHOT:shaded
//NOINTEGRATIONS
//RUNTIME_OPTIONS -Djava.util.logging.manager=org.jboss.logmanager.LogManager

package dev.snowdrop;

import dev.snowdrop.rewrite.service.RewriteService;
import dev.snowdrop.rewrite.config.RewriteConfig;
import dev.snowdrop.rewrite.ResultsContainer;
import org.openrewrite.DataTable;
import org.openrewrite.RecipeRun;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RewriteApp {
    public static void main(String[] args) throws Exception {

        String RECIPE_NAME = "org.openrewrite.java.format.AutoFormat";
        String APP_PATH = "demo";

        if (args.length > 0) {
            APP_PATH = args[0];
        }
        System.out.println("Processing application path: " + APP_PATH);

        RewriteConfig cfg = new RewriteConfig();
        cfg.setAppPath(Paths.get(APP_PATH));
        cfg.setFqNameRecipe(RECIPE_NAME);

        RewriteService scanner = new RewriteService(cfg);
        scanner.init();
        try {
            ResultsContainer results = scanner.runScanner();
            RecipeRun run = results.getRecipeRuns().get(RECIPE_NAME);

            Optional<Map.Entry<DataTable<?>, List<?>>> resultMap = run.getDataTables().entrySet().stream()
                .filter(entry -> entry.getKey().getName().contains("SourcesFileResults"))
                .findFirst();
            resultMap.ifPresent(dataTableListEntry -> System.out.println("SourcesFileResults size: " + dataTableListEntry.getValue().size()));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
