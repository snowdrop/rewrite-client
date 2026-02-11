# Rewrite scanner example using a Recipes YAML file

This example demonstrates how to use the RewriteScanner class to scan a Java Application using recipes defined in a YAML configuration file.

## Prerequisites

- Java 21
- Maven 3.9
- [jbang](https://www.jbang.dev/) (optional)

## Quick Start

### Recipe Configuration

The application uses a `rewrite.yml` file to define the OpenRewrite recipes. This file should be present in the project you're scanning, not in this example directory.

Create a `rewrite.yml` file in your target project with content like:

```yaml
---
type: specs.openrewrite.org/v1beta/recipe
name: dev.snowdrop.FormatCode
displayName: Format Code Recipe
description: A recipe to detect formatting issues in Java code
recipeList:
  - org.openrewrite.java.format.AutoFormat
  - org.openrewrite.java.format.RemoveUnusedImports
```

### Option 1: Running with jbang (Recommended)

The simplest way to run the RewriteApp is using jbang, which automatically handles dependencies:

```bash
# Run the app to scan a specific project
jbang run ./src/main/java/dev/snowdrop/RewriteApp.java /path/to/your/project

# Show help
jbang run ./src/main/java/dev/snowdrop/RewriteApp.java --help
```

### Option 2: Running with Maven (Traditional approach)

If you prefer to use Maven, follow these steps:

```bash
# Compile the project
mvn clean compile

# Run the application
mvn exec:java -Dexec.mainClass="dev.snowdrop.RewriteApp" -Dexec.args="/path/to/your/project"
```

Enjoy :-)