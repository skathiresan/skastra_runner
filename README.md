# Astra Runner

A complete Java application that automates execution of versioned Java libraries hosted in Artifactory. The application downloads the right artifact, executes it (either in-process or as a separate JVM), collects standardized reports, and summarizes results.

## 🏗️ Architecture

The application uses a **multi-module Gradle structure**:

- **`contracts/`** → Defines SPI (`Task` interface + DTOs)
- **`engine/`** → Artifact resolver, process executor, report aggregator  
- **`app/`** → Spring Boot CLI + optional REST endpoint

## 🚀 Quick Start

### Prerequisites

- Java 21
- Access to Artifactory with credentials
- Gradle 8.5+ (or use the included wrapper)

### Environment Setup

```bash
export ARTIFACTORY_URL="https://your-artifactory.com/artifactory/libs-release"
export ARTIFACTORY_USER="your-username"
export ARTIFACTORY_PASSWORD="your-password"
```

### Build the Application

```bash
./gradlew clean build
```

### Run CLI Mode

```bash
java -jar app/build/libs/astra-runner.jar \
  --ga="com.example:my-task" \
  --version="1.0.0" \
  --mode="CLI" \
  --argsJson='{"param1":"value1","param2":"value2"}' \
  --reportsDir="./reports"
```

### Run REST API Mode

```bash
java -jar app/build/libs/astra-runner.jar --rest
```

Then make HTTP requests:

```bash
curl -X POST http://localhost:8080/api/v1/execute \
  -H "Content-Type: application/json" \
  -d '{
    "groupArtifact": "com.example:my-task",
    "version": "1.0.0", 
    "mode": "SPI",
    "arguments": {"param1": "value1"},
    "reportsDir": "./reports"
  }'
```

## 📝 Usage

### Command Line Options

| Option | Description | Required | Default |
|--------|-------------|----------|---------|
| `--ga` | Group:Artifact identifier | Yes | - |
| `--version` | Version (exact, latest.release, or semver pattern) | Yes | - |
| `--mode` | Execution mode (CLI or SPI) | Yes | - |
| `--argsJson` | Runtime arguments as JSON string | No | `{}` |
| `--reportsDir` | Output directory for reports | No | `./reports` |
| `--workspace` | Workspace directory for temporary files | No | `./workspace` |
| `--timeout` | Execution timeout in milliseconds | No | `300000` |
| `--jvmArgs` | Additional JVM arguments (comma-separated) | No | - |
| `--rest` | Start REST API server instead of CLI execution | No | - |

### Execution Modes

#### CLI Mode
Executes the artifact as a separate JVM process using `java -jar <artifact>`. 

- Captures stdout/stderr to log files
- Supports JVM arguments and command-line parameters
- Returns process exit code

#### SPI Mode  
Loads the artifact in an isolated classloader and calls `com.acme.spi.Task#run()`.

- Uses ServiceLoader to discover Task implementations
- Executes in-process with isolated classpath
- Returns structured TaskResult

### Version Patterns

- **Exact version**: `1.0.0`, `2.1.3-SNAPSHOT`
- **Latest release**: `latest.release`
- **Semver patterns**: `1.+`, `2.1.+` (Gradle-style)

## 📊 Reports Generated

For each execution, the following standardized reports are created:

| File | Description |
|------|-------------|
| `run-summary.json` | Complete execution summary with metadata |
| `junit.xml` | JUnit-compatible test results |
| `summary.html` | Human-readable HTML summary |
| `results.json` | Raw TaskResult data |
| `stdout.log` | Standard output capture |
| `stderr.log` | Standard error capture |

## 🔌 SPI Implementation

To create a library compatible with SPI mode, implement the `Task` interface:

```java
package com.acme.spi;

import java.nio.file.Path;
import java.util.Map;

public interface Task {
    TaskResult run(Map<String, String> args, Path outDir) throws Exception;
    
    default String getName() {
        return this.getClass().getSimpleName();
    }
    
    default String getVersion() {
        return "unknown";
    }
}
```

Create a `META-INF/services/com.acme.spi.Task` file listing your implementation:

```
com.example.MyTaskImplementation
```

## 🏗️ CI/CD Integration

### Jenkins Pipeline

The included `Jenkinsfile` provides:

- Parameterized builds with GA, VERSION, MODE, and ARGS_JSON
- Automatic artifact resolution and execution
- JUnit XML publishing for test reporting
- HTML report publishing for execution summaries
- Artifact archival for all generated reports

### Usage in Jenkins

1. Create a new Pipeline job
2. Point to this repository
3. Configure parameters as needed
4. Set up Artifactory credentials in Jenkins credential store:
   - `artifactory-user`
   - `artifactory-password` 
   - `artifactory-url`

## 🔧 Configuration

### Application Properties

The application can be configured via `application.properties` or environment variables:

```properties
# Server configuration
server.port=8080

# Artifactory configuration
artifactory.url=${ARTIFACTORY_URL}
artifactory.user=${ARTIFACTORY_USER}
artifactory.password=${ARTIFACTORY_PASSWORD}

# Default execution settings
astra.default.timeout=300000
astra.default.workspace=./workspace
astra.default.reports=./reports
```

### Environment Variables

| Variable | Description |
|----------|-------------|
| `ARTIFACTORY_URL` | Artifactory repository URL |
| `ARTIFACTORY_USER` | Artifactory username |
| `ARTIFACTORY_PASSWORD` | Artifactory password |

## 🧪 Example Task Implementation

Here's a simple example of a Task implementation:

```java
package com.example.tasks;

import com.acme.spi.Task;
import com.acme.spi.TaskResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class HelloWorldTask implements Task {
    
    @Override
    public TaskResult run(Map<String, String> args, Path outDir) throws Exception {
        String name = args.getOrDefault("name", "World");
        String message = "Hello, " + name + "!";
        
        // Write output file
        Path outputFile = outDir.resolve("hello.txt");
        Files.writeString(outputFile, message);
        
        // Return success result
        return TaskResult.success()
            .withOutputFiles(List.of(outputFile.toString()))
            .withMetrics(Map.of("messageLength", message.length()));
    }
    
    @Override
    public String getName() {
        return "HelloWorldTask";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
}
```

## 🏃‍♂️ Development

### Building from Source

```bash
git clone <repository-url>
cd astra-runner
./gradlew clean build
```

### Running Tests

```bash
./gradlew test
```

### Building Distribution

```bash
./gradlew bootJar
```

The executable JAR will be created at `app/build/libs/astra-runner.jar`.

## 📋 Requirements Met

- ✅ **Multi-module Gradle structure** with contracts, engine, and app modules
- ✅ **Java 21 toolchain** with proper configuration
- ✅ **Artifactory integration** with authentication and dependency resolution
- ✅ **Dual execution modes** (CLI and SPI) with isolated classloading
- ✅ **Standardized reporting** with JSON, JUnit XML, and HTML outputs
- ✅ **Jenkins integration** with parameterized pipeline and artifact publishing
- ✅ **Reproducible execution** with version resolution, checksums, and metadata
- ✅ **No Docker dependencies** - runs directly on JVM

## 🔍 Troubleshooting

### Common Issues

1. **Artifactory Authentication Failed**
   - Verify `ARTIFACTORY_USER` and `ARTIFACTORY_PASSWORD` environment variables
   - Check Artifactory URL is accessible

2. **Artifact Not Found**
   - Verify the group:artifact identifier is correct
   - Check if the version exists in Artifactory
   - Try with `latest.release` to test connectivity

3. **SPI Mode: No Task Implementation Found**
   - Ensure the JAR contains a proper `META-INF/services/com.acme.spi.Task` file
   - Verify the Task implementation class is in the JAR
   - Check classpath and dependency resolution

4. **CLI Mode: Process Execution Failed**
   - Verify the JAR is executable (`java -jar` compatible)
   - Check JVM arguments and command-line parameter format
   - Review stdout.log and stderr.log for error details

### Logging

Enable debug logging:

```bash
java -jar astra-runner.jar --logging.level.com.acme.astra=DEBUG ...
```

## 📄 License

This project is licensed under the MIT License.# skastra_runner
