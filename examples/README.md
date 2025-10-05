# Sample Task Implementation

This directory contains example Task implementations that can be used to test the Astra Runner.

## HelloWorldTask

A simple task that demonstrates the SPI interface:

- Accepts a `name` parameter (defaults to "World")
- Creates a greeting message
- Writes output to a file
- Returns success with metrics

## Building the Sample

```bash
cd examples/sample-task
./gradlew build publishToMavenLocal
```

## Testing with Astra Runner

```bash
# Test SPI mode
java -jar ../../app/build/libs/astra-runner.jar \
  --ga="com.example:sample-task" \
  --version="1.0.0" \
  --mode="SPI" \
  --argsJson='{"name":"Astra"}' \
  --reportsDir="./test-reports"

# Test CLI mode (if the JAR has a main method)
java -jar ../../app/build/libs/astra-runner.jar \
  --ga="com.example:sample-task" \
  --version="1.0.0" \
  --mode="CLI" \
  --argsJson='{"name":"Astra"}' \
  --reportsDir="./test-reports"
```