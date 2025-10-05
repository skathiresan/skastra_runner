# Development Guide

This guide covers development setup, architecture details, and contribution guidelines for the Astra Runner project.

## Architecture Overview

### Module Structure

```
astra-runner/
├── contracts/          # SPI interfaces and DTOs
│   └── src/main/java/com/acme/
│       ├── spi/        # Task interface
│       └── astra/model/ # Data models
├── engine/             # Core execution engine
│   └── src/main/java/com/acme/astra/engine/
│       ├── resolver/   # Artifact resolution
│       ├── executor/   # Task execution
│       ├── report/     # Report generation
│       └── AstraEngine.java
├── app/                # Spring Boot application
│   └── src/main/java/com/acme/astra/app/
│       ├── cli/        # Command line interface
│       ├── rest/       # REST API
│       └── AstraRunnerApplication.java
└── examples/           # Sample implementations
    └── sample-task/    # Example Task implementation
```

### Key Components

#### ArtifactResolver
- Uses Gradle's dependency resolution via ProcessBuilder
- Authenticates with Artifactory using environment credentials
- Supports version patterns: exact, latest.release, semver
- Calculates SHA256 checksums for reproducibility
- Resolves transitive dependencies

#### TaskExecutor
- Supports two execution modes:
  - **CLI Mode**: Executes JARs as separate processes via ProcessBuilder
  - **SPI Mode**: Loads JARs in isolated URLClassLoader and invokes Task interface
- Captures stdout/stderr for CLI mode
- Handles timeouts and process management
- Provides structured error handling

#### ReportGenerator
- Generates standardized outputs:
  - `run-summary.json`: Complete execution metadata
  - `junit.xml`: JUnit-compatible test results
  - `summary.html`: Human-readable report
  - `results.json`: Raw TaskResult data
- Supports HTML templating for custom reports
- Archives all execution artifacts

## Development Setup

### Prerequisites

- Java 21 JDK
- Gradle 8.5+ (or use included wrapper)
- IDE with Java support (IntelliJ IDEA recommended)
- Access to Artifactory instance for testing

### Environment Configuration

Create a `.env` file (not committed to git):

```bash
ARTIFACTORY_URL=https://your-artifactory.com/artifactory/libs-release
ARTIFACTORY_USER=your-username
ARTIFACTORY_PASSWORD=your-password
```

Source it before development:

```bash
source .env
```

### IDE Setup

#### IntelliJ IDEA

1. Import as Gradle project
2. Set Project SDK to Java 21
3. Enable annotation processing
4. Install recommended plugins:
   - Spring Boot
   - Gradle
   - JSON Schema

#### VS Code

1. Install Java Extension Pack
2. Install Spring Boot Extension Pack
3. Configure Java 21 in settings
4. Use integrated terminal for Gradle commands

### Building and Testing

```bash
# Full build with tests
./gradlew build

# Run tests only
./gradlew test

# Run tests with coverage
./gradlew test jacocoTestReport

# Build without tests (faster)
./gradlew assemble

# Clean everything
./gradlew clean
```

### Local Development Workflow

1. **Build the application**:
   ```bash
   ./astra.sh build
   ```

2. **Run sample task**:
   ```bash
   ./astra.sh example
   ```

3. **Test CLI mode**:
   ```bash
   ./astra.sh run --ga="com.example:sample-task" --version="1.0.0" --mode="CLI" --argsJson='{"name":"Test"}'
   ```

4. **Test REST API**:
   ```bash
   ./astra.sh server
   # In another terminal:
   curl -X POST http://localhost:8080/api/v1/execute \
     -H "Content-Type: application/json" \
     -d '{"groupArtifact":"com.example:sample-task","version":"1.0.0","mode":"SPI","arguments":{"name":"REST"}}'
   ```

## Testing Strategy

### Unit Tests
- Located in `src/test/java` for each module
- Use JUnit 5 and Mockito
- Focus on individual component behavior
- Mock external dependencies (Artifactory, file system)

### Integration Tests
- Test complete execution flows
- Use TestContainers for Artifactory simulation
- Verify report generation and file outputs
- Test both CLI and SPI execution modes

### End-to-End Tests
- Use real Artifactory instance (test environment)
- Verify Jenkins pipeline integration
- Test error handling and edge cases
- Performance and timeout testing

## Debugging

### Common Debug Scenarios

#### Artifact Resolution Issues
```bash
# Enable debug logging
java -jar astra-runner.jar --logging.level.com.acme.astra=DEBUG \
  --ga="com.example:problematic-artifact" --version="latest.release" --mode="SPI"
```

#### SPI ClassLoader Issues
- Verify META-INF/services file exists and is correct
- Check Task implementation is public and has no-arg constructor
- Ensure all dependencies are available in classpath

#### CLI Process Issues
- Check JAR has proper Main-Class manifest
- Verify JVM arguments are correctly formatted
- Review stdout.log and stderr.log files

### Remote Debugging

For debugging the application:

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
  -jar astra-runner.jar --rest
```

Then connect your IDE debugger to port 5005.

## Performance Considerations

### Memory Management
- TaskExecutor uses isolated ClassLoaders that are properly closed
- Temporary files are cleaned up after execution
- Large artifacts are streamed rather than loaded entirely in memory

### Concurrency
- REST API supports concurrent executions
- Each execution gets isolated workspace directory
- Thread-safe logging and report generation

### Artifact Caching
- Consider implementing local artifact cache
- Reuse resolved dependencies across executions
- Cache metadata to speed up version resolution

## Security Considerations

### Credential Management
- Never commit credentials to git
- Use environment variables or secure credential stores
- Support Jenkins credential binding
- Consider using Artifactory API keys instead of passwords

### Code Execution Safety
- SPI mode uses isolated ClassLoaders
- CLI mode executes in separate processes
- Validate user inputs and file paths
- Limit execution timeouts to prevent resource exhaustion

### Dependency Management
- Regularly update dependencies for security patches
- Use Gradle dependency verification
- Scan artifacts for known vulnerabilities

## Contributing

### Code Style
- Follow Google Java Style Guide
- Use 4-space indentation
- Maximum line length: 120 characters
- Use meaningful variable and method names

### Commit Guidelines
- Use conventional commit format: `type(scope): description`
- Types: feat, fix, docs, style, refactor, test, chore
- Keep commits atomic and focused
- Write clear commit messages

### Pull Request Process
1. Create feature branch from main
2. Implement changes with tests
3. Ensure all tests pass
4. Update documentation if needed
5. Submit PR with clear description
6. Address review feedback
7. Squash commits before merge

### Documentation
- Update README.md for user-facing changes
- Add JavaDoc for public APIs
- Update this DEVELOPMENT.md for architectural changes
- Include examples for new features

## Release Process

### Version Management
- Use semantic versioning (MAJOR.MINOR.PATCH)
- Tag releases in git
- Update version in build.gradle.kts
- Generate changelog from commit history

### Building Releases
```bash
# Update version
./gradlew build -Pversion=1.1.0

# Create release artifacts
./gradlew bootJar publishToMavenLocal

# Tag release
git tag -a v1.1.0 -m "Release version 1.1.0"
git push origin v1.1.0
```

### Deployment
- Automated via Jenkins pipeline
- Deploy to artifact repository
- Update documentation
- Notify users of new release

## Troubleshooting

### Build Issues
- Clean gradle cache: `rm -rf ~/.gradle/caches`
- Refresh dependencies: `./gradlew --refresh-dependencies`
- Check Java version: `java --version`

### Runtime Issues
- Check environment variables are set
- Verify Artifactory connectivity
- Review application logs
- Check file permissions on workspace directories

### Testing Issues
- Ensure test resources are available
- Check test configuration properties
- Review test isolation (clean up between tests)
- Verify mock configurations are correct