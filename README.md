[![GitHub Actions Status](<https://img.shields.io/github/actions/workflow/status/snowdrop/rewrite-client/build-test.yml?branch=main&logo=GitHub&style=for-the-badge>)](https://github.com/snowdrop/rewrite-client/actions/workflows/build-test.yml)
[![License](https://img.shields.io/github/license/snowdrop/rewrite-client?style=for-the-badge&logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)
[![Version](https://img.shields.io/maven-central/v/dev.snowdrop.openrewrite/rewrite-client?logo=apache-maven&style=for-the-badge)](https://search.maven.org/artifact/dev.snowdrop.openrewrite/rewrite-client)

> [!WARNING]
> This formatter is experimental and under active development. Please report issues or suggestions on: https://github.com/snowdrop/rewrite-client/[GitHub]

## Openrewrite Quarkus client and library

This project supports to execute Openrewrite Recipe(s) without the need to use the maven goal `rewrite:dryRun` or `rewrite:run` according to the following scenario.

- Use the FQName of the recipe as parameter: `-r or --recipe <FQName_recipe>`. Example: `-r org.openrewrite.java.format.AutoFormat`. The tool will try to find the class of the recipe from the classes loaded using the runtime classpath
- The fields of the Recipe can be defined using the option `-o or --options "k=v,k=v,...`. Example: `-o annotationPattern=@org.springframework.boot.autoconfigure.SpringBootApplication`
- If Recipe is packaged in another JAR file, then provide its PATH or Maven GAV using `--jar <PATH_OR_GAV>`. Example: `--jar dev.snowdrop:openrewrite-recipes:1.0.0-SNAPSHOT test-project/demo-spring-boot-todo-app -r dev.snowdrop.mtool.openrewrite.java.search.FindAnnotations`
- The recipes can also be configured using a YAML recipe file and option `-c or --config <REWRITE_YAML_NAME>`. Example: `-c rewrite.yml`

It can be used as described hereafter using the Quarkus Picocli Client or the jar file as `library` released on maven central.

## Prerequisite

- JDK 21 
- Apache maven 3.9

## Instructions 

### To use the Rewrite scanner

To use the  java project if you import the following dependency
```xml
    <groupId>dev.snowdrop.openrewrite</groupId>
    <artifactId>rewrite-client</artifactId>
    <version>0.1.0</version>
```
Next configure the `RewriteScanner` to issue a scan of a java application as described hereafter
```java
Config cfg = new Config();
cfg.setAppPath(Paths.get("<PATH_TO_JAVA_PROJECT>"));
cfg.setActiveRecipes(List.of("FQNAME_OF_THE_RECIPE"));
cfg.setRecipeOptions(Set.of("annotationPattern=@org.springframework.boot.autoconfigure.SpringBootApplication,matchMetaAnnotations=false"));

RewriteScanner scanner = new RewriteScanner(cfg);
scanner.init();
ResultsContainer results = scanner.run();

// Use the results object to access the Datatables, Changeset, etc
RecipeRun run = results.getRecipeRuns().get("FQNAME_OF_THE_RECIPE");
Optional<Map.Entry<DataTable<?>, List<?>>> resultMap = run.getDataTables().entrySet().stream()
    .filter(entry -> entry.getKey().getName().contains("SearchResults"))
    .findFirst();
assertTrue(resultMap.isPresent());

List<?> rows = resultMap.get().getValue();
assertEquals(1, rows.size());

SearchResults.Row record = (SearchResults.Row)rows.getFirst();
assertEquals("src/main/java/com/todo/app/AppApplication.java",record.getSourcePath());
assertEquals("@SpringBootApplication",record.getResult());
assertEquals("Find annotations `@org.springframework.boot.autoconfigure.SpringBootApplication,matchMetaAnnotations=false`",record.getRecipe());
```

> [!TRICK]
> See the sample [rewritescanner](sample/rewritescanner)  as a maven example to start to play with the scanner and don't forget to change the path of the config
> `cfg.setAppPath(Paths.get("/PATH/TO/JAVA/test-project/simple"));` !

### To use instead the client locally

The client can be installed using the [jbang tool](https://www.jbang.dev/) with the following command
```shell
jbang app install openrewrite@snowdrop/rewrite-client
```
When done, execute by example this command to auto format the java project:
```shell
openrewrite <PATH_OF_JAVA_THE_APPLICATION> -r <FQNAME_OF_THE_RECIPE>
openrewrite test-project/simple -r org.openrewrite.java.format.AutoFormat
```

> [!NOTE]
> To install a released version, append to the reference of the version
```shell
jbang app install openrewrite@snowdrop/rewrite-client/v0.1.0
```

### To start with the code

Git clone this project and compile the project. Next launch the Quarkus Picocli client using the command: `mvn quarkus:dev`

```shell
mvn clean install
mvn quarkus:dev -Dquarkus.args="test-project/simple -r org.openrewrite.java.format.AutoFormat"
```

> [!NOTE]
> You can also run the application using the uber jar file and command: `java -jar test-project/simple -r org.openrewrite.java.format.AutoFormat`
> Don't hesitate too to create an env variable:
```shell
set qrun java -jar target/quarkus-app/quarkus-run.jar
$qrun test-project/demo-spring-boot-todo-app -r org.openrewrite.java.search.FindAnnotations
$qrun test-project/demo-spring-boot-todo-app -r org.openrewrite.java.search.FindAnnotations -o annotationPattern=@org.springframework.boot.autoconfigure.SpringBootApplication,matchMetaAnnotations=false
$qrun test-project/demo-spring-boot-todo-app -r org.openrewrite.maven.search.FindDependency -o groupId=org.springframework.boot,artifactId=spring-boot-starter-data-jpa,version=3.5.3
```

## Options

As mentioned earlier, the client supports different options described hereafter.

### Recipe with parameter(s)

If the recipe, which is a Java class, can be configured using parameters, then declare them using the format `key=value` with the client's option `-o`.

```shell
mvn quarkus:dev -Dquarkus.args="test-project/demo-spring-boot-todo-app -r org.openrewrite.java.search.FindAnnotations -o annotationPattern=@org.springframework.boot.autoconfigure.SpringBootApplication,matchMetaAnnotations=false"

or 

openrewrite test-project/demo-spring-boot-todo-app -r org.openrewrite.java.search.FindAnnotations -o annotationPattern=@org.springframework.boot.autoconfigure.SpringBootApplication,matchMetaAnnotations=false
```

> [!TIP]
> Multiple parameters can be provided as a comma-separated list

### Recipes declared in a YAML file

You can also use a YAML recipes file and pass it using the client's option `-c`:
```shell
mvn quarkus:dev -Dquarkus.args="test-project/demo-spring-boot-todo-app -c rewrite.yml"

or 

openrewrite test-project/demo-spring-boot-todo-app -c rewrite.yml
```

### Recipes packaged in a separate jar

You can also load the recipes from an additional jar file using the Maven GAV coordinates and client's option `--jar`
```shell
mvn quarkus:dev -Dquarkus.args="--jar dev.snowdrop:openrewrite-recipes:1.0.0-SNAPSHOT test-project/demo-spring-boot-todo-app -r dev.snowdrop.mtool.openrewrite.java.search.FindAnnotations -o pattern=@org.springframework.boot.autoconfigure.SpringBootApplication,matchId=1234"

or

openrewrite --jar dev.snowdrop:openrewrite-recipes:1.0.0-SNAPSHOT test-project/demo-spring-boot-todo-app -r dev.snowdrop.mtool.openrewrite.java.search.FindAnnotations -o pattern=@org.springframework.boot.autoconfigure.SpringBootApplication,matchId=1234
```

## For the developers

The project and jar file can be debugged if you configure the agentlib 
```shell
set qdebug java -jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address="*:5005" target/quarkus-app/quarkus-run.jar

$qdebug test-project/demo-spring-boot-todo-app -r org.openrewrite.java.search.FindAnnotations
```

Enjoy :-)
