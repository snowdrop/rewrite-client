package dev.snowdrop.rewrite.recipe.yaml.quarkus_resteasy;

import dev.snowdrop.rewrite.cli.BaseTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.wildfly.common.Assert;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class RecipesYamlTest extends BaseTest {

    @Test
    void testRESTEasyMigration() throws Exception {

        String appPath = "test-project/quarkus-resteasy-classic-app";
        String recipeName = "dev.snowdrop.openrewrite.java.QuarkusToQuarkus";

        // Configure the application to scan and recipe to be executed
        cfg.setAppPath(Paths.get(appPath));
        cfg.setFqNameRecipe(recipeName);
        cfg.setYamlRecipesPath("rewrite.yml");

        var results = rewriteCmd.execute(cfg);
        RecipeRun run = results.getRecipeRuns().get(recipeName);

        Assert.assertTrue(run.getChangeset().getAllResults().size() == 3);
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
        assertNotNull(changes.get(0).getAfter());
        assertEquals("src/main/java/com/demo/library/BookResource.java", changes.get(0).getAfter().getSourcePath().toString());

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
        assertNotNull(changes.get(0).getAfter());
        assertEquals("pom.xml", changes.get(1).getAfter().getSourcePath().toString());

        /*
           EXPECT: Properties replaced  in resources/application.properties
             -quarkus.resteasy.gzip.enabled=true
             -quarkus.resteasy.gzip.max-input=10M
             +quarkus.rest.gzip.enabled=true
             +quarkus.rest.gzip.max-input=10M
        */
        assertNotNull(changes.get(2).getAfter());
        assertEquals("src/main/resources/application.properties", changes.get(2).getAfter().getSourcePath().toString());
    }
}