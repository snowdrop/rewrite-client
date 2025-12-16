## Instructions

Prerequisite:
- JDK 21 and maven 3.9

Compile the project and run `quarkus:dev`

```shell
mvn quarkus:dev -Dquarkus.args="test-project/simple org.openrewrite.java.format.AutoFormat"
mvn quarkus:dev -Dquarkus.args="test-project/demo-spring-boot-todo-app org.openrewrite.java.search.FindAnnotations"
```

> *TODO*
> Load the Recipe's class from the FQName and instantiate the object
> Pass the parameters to the Recipe object initialized (class introspection, etc.)
> Load the recipe(s) from an external jar file

## Trick for the developers

```shell
set qdebug java -jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address="*:5005" target/quarkus-app/quarkus-run.jar
set qrun java -jar target/quarkus-app/quarkus-run.jar

$qdebug test-project/demo-spring-boot-todo-app org.openrewrite.java.search.FindAnnotations
$qrun test-project/demo-spring-boot-todo-app org.openrewrite.java.search.FindAnnotations
```

## Issue

- FIXED: There is a Java Module issue with Maven and Quarkus:dev as command works using `java -jar` - https://github.com/ch007m/rewrite-standalone-cli/issues/1