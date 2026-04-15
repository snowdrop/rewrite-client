package dev.snowdrop.rewrite.recipe.yaml.quarkus_resteasy;

import dev.snowdrop.rewrite.service.RewriteService;
import dev.snowdrop.rewrite.BaseTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.wildfly.common.Assert;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class RecipesYamlTest extends BaseTest {

    @Test
    void testRESTEasyMigration() throws Exception {

        String appPath = "../test-project/quarkus-resteasy-classic-app";
        String recipeName = "dev.snowdrop.openrewrite.java.QuarkusToQuarkus";

        // Configure the application to scan and recipe to be executed
        cfg.setAppPath(Paths.get(appPath));
        cfg.setFqNameRecipe(recipeName);
        cfg.setYamlRecipesPath("rewrite.yml");

        RewriteService rewriteService = new RewriteService(cfg);
        rewriteService.init();
        var results = rewriteService.runScanner();
        RecipeRun run = results.getRecipeRuns().get(recipeName);

        Assert.assertFalse(run.getChangeset().getAllResults().isEmpty());
        List<Result> changes = run.getChangeset().getAllResults();

        /*
           EXPECT: Annotations changed in class: src/main/java/com/demo/library/BookResource.java
             -import org.jboss.resteasy.annotations.jaxrs.HeaderParam;
             -import org.jboss.resteasy.annotations.jaxrs.PathParam;
             -import org.jboss.resteasy.annotations.jaxrs.QueryParam;
             +import org.jboss.resteasy.reactive.RestHeader;
             +import org.jboss.resteasy.reactive.RestPath;
             +import org.jboss.resteasy.reactive.RestQuery;
         */
        boolean found = changes.stream()
                .anyMatch(r -> {
                    String diff = r.diff();
                    return diff.contains("-import org.jboss.resteasy.annotations.jaxrs.HeaderParam;")
                           && diff.contains("+import org.jboss.resteasy.reactive.RestHeader;")
                           && diff.contains("a/src/main/java/com/demo/library/BookResource.java");
                });
        assertTrue(found, "Expected new import not found in any changeSet results");

        /*
            EXPECT: GAVs and version replaced in: pom.xml
             - <quarkus.platform.version>3.18.4</quarkus.platform.version>
             + <quarkus.platform.version>3.31.2</quarkus.platform.version>
                <dependency>
                    <groupId>io.quarkus</groupId>
             -      <artifactId>quarkus-resteasy</artifactId>
             +      <artifactId>quarkus-rest</artifactId>
                </dependency>
                <dependency>
                    <groupId>io.quarkus</groupId>
             -      <artifactId>quarkus-resteasy-jackson</artifactId>
             +      <artifactId>quarkus-rest-jackson</artifactId>
                </dependency>
         */
        found = changes.stream()
                .anyMatch(r -> {
                    String diff = r.diff();
                    return diff.contains("<artifactId>quarkus-rest</artifactId>")
                            && diff.contains("<artifactId>quarkus-rest-jackson</artifactId>");
                });
        assertTrue(found, "Expected new artifacts within the pom.xml file not found in any changeSet results");

        /*
           EXPECT: Properties replaced  in resources/application.properties
             -quarkus.resteasy.gzip.enabled=true
             -quarkus.resteasy.gzip.max-input=10M
             +quarkus.rest.gzip.enabled=true
             +quarkus.rest.gzip.max-input=10M
        */
        found = changes.stream()
                .anyMatch(r -> {
                    String diff = r.diff();
                    return diff.contains("-quarkus.resteasy.gzip.enabled=true")
                            && diff.contains("-quarkus.resteasy.gzip.max-input=10M")
                            && diff.contains("+quarkus.rest.gzip.enabled=true")
                            && diff.contains("+quarkus.rest.gzip.max-input=10M");
                });
        assertTrue(found, "Expected properties changed not found in any changeSet results");
    }
}