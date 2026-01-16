///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.snowdrop.openrewrite:rewrite-client:0.1.1
//NOINTEGRATIONS
//RUNTIME_OPTIONS -Djava.util.logging.manager=org.jboss.logmanager.LogManager

package dev.snowdrop;

import dev.snowdrop.openrewrite.cli.RewriteScanner;
import dev.snowdrop.openrewrite.cli.model.Config;
import dev.snowdrop.openrewrite.cli.model.ResultsContainer;
import org.openrewrite.DataTable;
import org.openrewrite.RecipeRun;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RewriteApp {
    public static void main(String[] args) {
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("--help"))) {
            printUsage();
            return;
        }

        String RECIPE_NAME = "dev.snowdrop.RecipeExample";

        // Get the path from command line argument or use default
        String projectPath = getProjectPath(args);

        Config cfg = new Config();
        cfg.setAppPath(Paths.get(projectPath));
        cfg.setYamlRecipes("rewrite.yml");

        RewriteScanner scanner = new RewriteScanner(cfg);
        scanner.init();
        try {
            ResultsContainer results = scanner.run();
            RecipeRun run = results.getRecipeRuns().get(RECIPE_NAME);

            Optional<Map.Entry<DataTable<?>, List<?>>> resultMap = run.getDataTables().entrySet().stream()
                .filter(entry -> entry.getKey().getName().contains("SourcesFileResults"))
                .findFirst();
            resultMap.ifPresent(dataTableListEntry -> System.out.println("SourcesFileResults size: " + dataTableListEntry.getValue().size()));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the project path from command line arguments or return default path
     */
    private static String getProjectPath(String[] args) {
        if (args.length > 0 && !args[0].startsWith("-")) {
            return args[0];
        } else {
            throw new IllegalArgumentException("Missing required path argument !");
        }
    }

    /**
     * Print usage information
     */
    private static void printUsage() {
        System.out.println("Usage: RewriteApp [OPTIONS] [PROJECT_PATH]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  PROJECT_PATH    Path to the project to scan (default: current directory)");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help      Show this help message and exit");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  RewriteApp                              # Scan current directory");
        System.out.println("  RewriteApp /path/to/my/project          # Scan specific project");
        System.out.println("  RewriteApp --help                       # Show help");
    }
}
