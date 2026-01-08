# Rewrite scanner example

This example demonstrates how to use the `RewriteScanner` class to scan a Java Application using a FQName recipe defined using the `Config`.

## Prerequisites

- Java 21
- Maven 3.9
- [jbang](https://www.jbang.dev/) (optional)

## Quick Start

### Option 1: Running with jbang (Recommended)

The simplest way to run the RewriteApp is using jbang, which automatically handles dependencies:

```bash
# Run the app to scan a specific project
jbang run ./src/main/java/dev/snowdrop/RewriteApp.java /PATH/TO/YOUR/APP
```

> [!TIP]
> You can use the demo java application as project to scan and omit in this case to pass an argument to the command !

### Option 2: Running with Maven (Traditional approach)

If you prefer to use Maven, follow these steps:

```bash
# Compile the project
mvn clean compile

# Run the application
mvn exec:java -Dexec.mainClass="dev.snowdrop.RewriteApp"
or
mvn exec:java -Dexec.mainClass="dev.snowdrop.RewriteApp" -Dexec.args="/path/to/your/project"
```

Enjoy :-)