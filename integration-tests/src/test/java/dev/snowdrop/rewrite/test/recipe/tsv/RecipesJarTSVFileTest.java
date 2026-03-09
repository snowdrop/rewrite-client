package dev.snowdrop.rewrite.test.recipe.tsv;

import dev.snowdrop.rewrite.test.BaseReflectionTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecipesJarTSVFileTest extends BaseReflectionTest {

    /*
       We got the error reported to openrewrite: https://github.com/openrewrite/rewrite-spring-to-quarkus/issues/77
       java.lang.RuntimeException: Error while visiting src/main/java/com/todo/app/controller/TaskController.java: java.lang.IllegalArgumentException:
         Unable to find classpath resource dependencies beginning with: 'jakarta.inject-api' in [jakarta.ws.rs-api-3.1.0]
         org.openrewrite.java.JavaParser.dependenciesFromResources(JavaParser.java:171)
         org.openrewrite.java.JavaParser$Builder.classpathFromResources(JavaParser.java:318)
         org.openrewrite.java.ReplaceAnnotation$1.visitAnnotation(ReplaceAnnotation.java:86)

     */
    //@Disabled
    @Test
    void useRecipesJarToUpgradeDependencies() throws Exception {

        String appPath = "../test-project/spring-boot-todo-app";
        String recipeName = "org.openrewrite.quarkus.spring.SpringBootToQuarkus";

        // Configure the application to scan and recipe to be executed
        proxy.setProjectRoot(Paths.get(appPath));
        proxy.setFQRecipeName(recipeName);
        proxy.setAdditionalJarPaths(List.of(
                "dev.snowdrop.openrewrite:service:jar:shaded:0.2.12-SNAPSHOT",
                "org.openrewrite.recipe:rewrite-spring-to-quarkus:0.6.0",
                "org.openrewrite.recipe:rewrite-java-dependencies:1.51.0"
        ));

        proxy.runScanner();
        Object results = proxy.runScanner();
        Assertions.assertNotNull(results);
    }
}