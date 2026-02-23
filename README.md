# OpenRewrite Client and library

[![GitHub Actions Status](https://img.shields.io/github/actions/workflow/status/snowdrop/rewrite-client/build-test.yml?branch=main&logo=GitHub&style=for-the-badge)](https://github.com/snowdrop/rewrite-client/actions/workflows/build-test.yml)
[![License](https://img.shields.io/github/license/snowdrop/rewrite-client?style=for-the-badge&logo=apache)](https://www.apache.org/licenses/LICENSE-2.0)
[![Version](https://img.shields.io/maven-central/v/dev.snowdrop.openrewrite/rewrite-client?logo=apache-maven&style=for-the-badge)](https://search.maven.org/artifact/dev.snowdrop.openrewrite/rewrite-client)

A Java library and CLI tool for executing OpenRewrite recipes programmatically, without requiring Maven plugin execution.

## Overview

Rewrite is a powerful framework for automated code refactoring.
Traditionally, it's used via Maven goals (`mvn rewrite:run` or `mvn rewrite:dryRun`), but this project provides an alternative approach that allows you to:

- **Execute recipes programmatically** from your Java code
- **Run recipes via CLI** without Maven
- **Process results directly** without parsing console logs or csv files generated.
- **Integrate easily** into custom migration tools and workflows

## Quick Start

### As a Library

Add the dependency to your project:

```xml
<dependency>
    <groupId>dev.snowdrop.openrewrite</groupId>
    <artifactId>rewrite-client</artifactId>
    <version>0.2.4</version>
</dependency>
```

Use the `RewriteService` to process a Java project:

```java
import dev.snowdrop.openrewrite.RewriteService;
import dev.snowdrop.openrewrite.Config;
import dev.snowdrop.openrewrite.ResultsContainer;

Config cfg = new Config();
cfg.setAppPath(Paths.get("/path/to/your/project"));
cfg.setRecipes(List.of("org.openrewrite.java.format.AutoFormat"));

RewriteService scanner = new RewriteService(cfg);
scanner.init();
ResultsContainer results = scanner.run();

// Access results
RecipeRun run = results.getRecipeRuns().get("org.openrewrite.java.format.AutoFormat");
// Process datatables, changesets, etc.
```

### As a CLI Tool

Install using [jbang](https://www.jbang.dev/):

```bash
jbang app install rewrite@snowdrop/rewrite-client
```
> [!NOTE]
> To install a released version, append to the reference of the version
```shell
jbang app install rewrite@snowdrop/rewrite-client/0.2.4
```

Run a recipe:

```bash
rewrite /path/to/project -r org.openrewrite.java.format.AutoFormat
```

Run a recipe without the `dryrun` mode:

```bash
rewrite /path/to/project -r org.openrewrite.java.format.AutoFormat -d false
```


## Usage
Important Considerations:

When using the rewrite-client, keep these key points in mind:

- **Recipe Resolution**: Recipes are specified by their fully qualified class name (e.g., org.openrewrite.java.format.AutoFormat).
  The tool loads recipe classes from the runtime classpath, so ensure the recipe's JAR is available.

- **Parameter Configuration**: Recipe fields can be configured using comma-separated key-value pairs.
  Values are automatically type-converted to match the recipe's field types (String, boolean, int, etc.).

- **External Recipe JARs**: If your recipe is packaged in a separate JAR file, you can load it dynamically using either a local file path or Maven GAV coordinates.
  The tool will resolve and add it to the classpath before execution.

- **YAML Configuration Files**: For complex recipe configurations or multiple recipes, YAML files provide a cleaner alternative.
  They support the OpenRewrite YAML specification format and allow you to define recipe chains with parameters.


### Basic Recipe Execution

Execute a recipe by its fully qualified class name:

```bash
rewrite /path/to/project -r org.openrewrite.java.format.AutoFormat
```

**In code:**

```java
Config cfg = new Config();
cfg.setAppPath(Paths.get("/path/to/project"));
cfg.setFqNameRecipe("org.openrewrite.java.format.AutoFormat");

RewriteService scanner = new RewriteService(cfg);
scanner.init();
ResultsContainer results = scanner.run();
```

### Recipe with Parameters

Many recipes accept configuration parameters. Pass them using the `-o` option:

```bash
rewrite /path/to/project \
  -r org.openrewrite.java.search.FindAnnotations \
  -o annotationPattern=@org.springframework.boot.autoconfigure.SpringBootApplication,matchMetaAnnotations=false
```

**In code:**

```java
cfg.setRecipeOptions(Set.of(
    "annotationPattern=@org.springframework.boot.autoconfigure.SpringBootApplication",
    "matchMetaAnnotations=false"
));
```

**Multiple parameters** are separated by commas in the format `key=value,key2=value2`.

### Using a YAML Configuration File

Define recipes in a YAML file:

```yaml
# rewrite.yml
type: specs.openrewrite.org/v1beta/recipe
name: com.example.MyRecipe
displayName: My Custom Recipe
recipeList:
  - org.openrewrite.java.format.AutoFormat
  - org.openrewrite.java.search.FindAnnotations:
      annotationPattern: '@SpringBootApplication'
```

Execute with:

```bash
rewrite /path/to/project -c rewrite.yml
```

**In code:**

```java
cfg.setConfigPath(Paths.get("rewrite.yml"));
```

### Loading External Recipe JARs

Load recipes from external JAR files using Maven GAV coordinates:

```bash
rewrite --jar dev.snowdrop:openrewrite-recipes:0.2.10-SNAPSHOT \
  /path/to/project \
  -r dev.snowdrop.custom.MyRecipe
```

Or use a local JAR path:

```bash
rewrite --jar /path/to/recipes.jar /path/to/project -r com.example.MyRecipe
```

## Processing Results

The `ResultsContainer` provides access to recipe execution results:

```java
ResultsContainer results = scanner.run();

// Get results for a specific recipe
RecipeRun run = results.getRecipeRuns().get("org.openrewrite.java.search.FindAnnotations");

// Fetch the DataTable matching the recipe executed for SearchResults
List<SearchResults.Row> rows = findDataTableRows(run, "SearchResults", SearchResults.Row.class);
SearchResults.Row record = rows.getFirst();
System.out.println("Found in: " + record.getSourcePath());
System.out.println("Match: " + record.getResult());

// Access changesets (code modifications)
// run.getChangeset()...
```

## Reuse resources already scanned

When you use the method `init()` of the `RewriteService`, then the library will create the following OpenRewrite class during the call to the method:

- `Environment` holding the resource loaders able to find Recipe classes from jar, classes loaded or Yaml
- `ExecutionContext` able to collect from the execution of the different Recipe the messages containing the Map of the DataTable, etc
- `LargeSourceSet` using different parsers able to read: Java, Maven, Properties, XML, JSON, etc files

It is nevertheless possible, to scan the resources one time and apply different recipes. In this case, we recommend you to create a singleton of the `RewriteService` and next to call separately the following methods:

```java
// Create a singleton instance of the RewriteService to allow to load only one time all the resources of a project to scan
RewriteService svc = createRewriteServiceInstance(cfg);

if (svc.isSourceSetInitialized()) {
	svc.createExecutionContext();
	svc.updateConfig(cfg);
} else {
	svc.init();
}

// Execute now a recipe
ResultsContainer run = svc.run();
```

## Development

### Prerequisites

- JDK 21
- Apache Maven 3.9+

### Building from Source

```bash
git clone https://github.com/snowdrop/rewrite-client.git
cd rewrite-client
mvn clean install
```

### Running in Development Mode

```bash
mvn quarkus:dev -Dquarkus.args="test-project/simple -r org.openrewrite.java.format.AutoFormat"
```

### Debugging

Configure the Java agent for debugging:

```bash
java -jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address="*:5005" \
  target/quarkus-app/quarkus-run.jar \
  test-project/simple -r org.openrewrite.java.format.AutoFormat
```

Then attach your debugger to port 5005.

## Examples

Check the [`sample`](sample) directory for different examples demonstrating the library usage. 
The folder includes a basic Java `demo` application 

### Find Spring Boot Annotations

```bash
openrewrite test-project/demo-spring-boot-todo-app \
  -r org.openrewrite.java.search.FindAnnotations \
  -o annotationPattern=@org.springframework.boot.autoconfigure.SpringBootApplication
```

### Find Maven Dependencies

```bash
openrewrite test-project/demo-spring-boot-todo-app \
  -r org.openrewrite.maven.search.FindDependency \
  -o groupId=org.springframework.boot,artifactId=spring-boot-starter-data-jpa,version=3.5.3
```

### Auto-format Code

```bash
rewrite test-project/simple -r org.openrewrite.java.format.AutoFormat
```

## Use Cases

This library is particularly useful for:

- **Migration Tools**: Integrate OpenRewrite scanning into custom migration frameworks
- **CI/CD Pipelines**: Run recipe checks without full Maven builds
- **Custom Tooling**: Build specialized refactoring or analysis tools
- **Batch Processing**: Execute recipes across multiple projects programmatically

## License

This project is licensed under the [Apache License 2.0](LICENSE).

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.