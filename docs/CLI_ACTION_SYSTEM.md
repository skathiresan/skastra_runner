# CLI Action System

The Astra Runner includes an extensible CLI action system that allows you to perform utility operations without executing artifacts. This system provides a clean way to add diagnostic, informational, and utility functions.

## Quick Usage

```bash
# Display system information
java -jar astra-runner.jar --action=system-info

# Show all arguments passed to the application
java -jar astra-runner.jar --action=show-args --ga=com.example:test --version=1.0.0

# Display comprehensive information
java -jar astra-runner.jar --action=info --args='{"format":"json"}'

# List all available actions
java -jar astra-runner.jar
```

## Available Actions

### system-info
Displays detailed system information including Java version, OS details, memory usage, and runtime arguments.

**Usage:**
```bash
java -jar astra-runner.jar --action=system-info
```

**Features:**
- Java version, vendor, home directory, VM details
- Operating system name, version, architecture
- Memory usage statistics (max, total, used, free)
- User and environment information
- All arguments passed to the action

### show-args
Displays all arguments passed to the application in various formats.

**Usage:**
```bash
java -jar astra-runner.jar --action=show-args --ga=com.example:test --version=1.0.0
java -jar astra-runner.jar --action=show-args --args='{"format":"json"}'
java -jar astra-runner.jar --action=show-args --args='{"format":"env"}'
```

**Supported Formats:**
- `table` - Human-readable table format (default)
- `json` - JSON format
- `env` - Environment variable export format
- `properties` - Java properties format

**Options:**
- `includeEmpty=true` - Include arguments with empty values

### info
Combines system information and argument display in a comprehensive format.

**Usage:**
```bash
java -jar astra-runner.jar --action=info
java -jar astra-runner.jar --action=info --args='{"format":"json","verbose":"true"}'
```

**Features:**
- Complete system information
- All arguments in organized format
- Runtime statistics
- Available in both human-readable and JSON formats

## Architecture

### CliAction Interface
All actions implement the `CliAction` interface:

```java
public interface CliAction {
    String getName();
    String getDescription();
    int execute(Map<String, String> args);
    void printUsage();
}
```

### ActionRegistry
The `ActionRegistry` manages all available actions:
- Auto-discovers actions via Spring's dependency injection
- Provides action lookup and execution
- Generates help information

### Integration with CLI
Actions are integrated into the main CLI through:
- `--action=<action-name>` parameter
- Automatic argument parsing and forwarding
- Exit code handling and logging

## TriggerJob Action JSON Configuration

The `trigger-job` action accepts a JSON configuration via the `--args` parameter. This section documents the complete structure and all available options.

### Basic JSON Structure

```json
{
  "parallel": "true|false",
  "dryRun": "true|false", 
  "format": "human|json",
  "workspace": "/path/to/workspace",
  "timeout": "300",
  "showArgs": "true|false",
  "jobs": [
    {
      "groupArtifact": "com.example:my-job",
      "version": "1.0.0",
      "args": ["--param1=value1", "--param2=value2"]
    }
  ]
}
```

### Configuration Options

#### Top-Level Options

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `parallel` | String | `"false"` | Execute jobs in parallel (`"true"`) or sequential (`"false"`) |
| `dryRun` | String | `"false"` | Download JARs but skip execution (`"true"`) or execute normally (`"false"`) |
| `format` | String | `"human"` | Output format: `"human"` for readable output, `"json"` for structured data |
| `workspace` | String | auto-generated | Custom workspace directory path (absolute or relative) |
| `timeout` | String | `"300"` | Timeout per job execution in seconds |
| `showArgs` | String | `"false"` | Display all available Astra Runner arguments before execution |
| `jobs` | Array | default jobs | Custom job configuration (see Job Configuration below) |

#### Job Configuration

Each job in the `jobs` array has the following structure:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `groupArtifact` | String | Yes | Maven coordinates in format `groupId:artifactId` |
| `version` | String | Yes | Version of artifact to download (supports `latest.release`) |
| `args` | Array of Strings | Yes | Command line arguments to pass to the JAR |

### Default Jobs Configuration

If no custom `jobs` are provided, the action uses these default jobs:

```json
{
  "jobs": [
    {
      "groupArtifact": "com.example:data-processor",
      "version": "latest.release",
      "args": ["--mode=batch", "--input=/data/input", "--output=/data/output"]
    },
    {
      "groupArtifact": "com.example:notification-service", 
      "version": "1.0.0",
      "args": ["--type=email", "--template=job-completion"]
    },
    {
      "groupArtifact": "com.example:cleanup-utility",
      "version": "latest.release", 
      "args": ["--target=/tmp", "--age=7d", "--dry-run=false"]
    }
  ]
}
```

### Example Configurations

#### 1. Basic Parallel Execution
```bash
java -jar astra-runner.jar --action=trigger-job --args='{"parallel":"true"}'
```

#### 2. Dry Run with JSON Output
```bash
java -jar astra-runner.jar --action=trigger-job --args='{"dryRun":"true","format":"json"}'
```

#### 3. Custom Single Job
```bash
java -jar astra-runner.jar --action=trigger-job --args='{
  "jobs": [{
    "groupArtifact": "com.mycompany:custom-processor",
    "version": "2.1.0", 
    "args": ["--config=/app/config.yml", "--mode=production"]
  }]
}'
```

#### 4. Multiple Custom Jobs with Options
```bash
java -jar astra-runner.jar --action=trigger-job --args='{
  "parallel": "true",
  "timeout": "600",
  "format": "json",
  "workspace": "/tmp/my-jobs",
  "jobs": [
    {
      "groupArtifact": "com.example:etl-job",
      "version": "3.0.0",
      "args": ["--source=database", "--target=warehouse", "--batch-size=1000"]
    },
    {
      "groupArtifact": "com.example:validation-job", 
      "version": "latest.release",
      "args": ["--strict=true", "--report-format=json"]
    }
  ]
}'
```

#### 5. With Astra Runner Parameters
```bash
java -jar astra-runner.jar \
  --action=trigger-job \
  --ga=com.parent:orchestrator \
  --version=1.0.0 \
  --jvmArgs="-Xmx2g,-Denv=prod" \
  --args='{"parallel":"true","showArgs":"true"}'
```

### JSON Output Format

When `format` is set to `"json"`, the action returns structured output:

```json
{
  "summary": {
    "workspace": "/path/to/workspace",
    "totalJobs": 3,
    "successfulJobs": 2, 
    "failedJobs": 1,
    "startTime": "2025-10-05T23:41:29.808782Z",
    "endTime": "2025-10-05T23:41:32.156443Z",
    "totalDurationMs": 2348
  },
  "results": [
    {
      "groupArtifact": "com.example:data-processor",
      "version": "latest.release",
      "jarPath": "./workspace/job-com.example_data-processor/data-processor-latest.release.jar",
      "args": ["--mode=batch", "--input=/data/input", "--output=/data/output"],
      "success": true,
      "exitCode": 0,
      "message": "Execution completed successfully",
      "startTime": "2025-10-05T23:41:29.811252Z",
      "endTime": "2025-10-05T23:41:31.156443Z", 
      "durationMs": 1345
    }
  ]
}
```

### Parameter Access

The trigger-job action has access to all Astra Runner CLI parameters:

- `ga` - Group and artifact coordinates from `--ga` parameter
- `version` - Version from `--version` parameter  
- `jvmArgs` - JVM arguments from `--jvmArgs` parameter
- All other CLI options and their values

Use `showArgs: "true"` to display all available parameters during execution.

### Error Handling

Invalid JSON configurations will result in:
- Parse errors with specific details about malformed JSON
- Validation errors for missing required fields
- Execution errors with job-specific failure details

The action uses fail-fast behavior - any configuration error stops execution before job processing begins.

## Creating New Actions

### 1. Implement CliAction Interface

```java
@Component
public class MyCustomAction implements CliAction {
    
    @Override
    public String getName() {
        return "my-action";
    }
    
    @Override
    public String getDescription() {
        return "Description of what this action does";
    }
    
    @Override
    public int execute(Map<String, String> args) {
        try {
            // Action implementation
            System.out.println("Executing my custom action");
            
            // Access arguments
            String param = args.get("someParam");
            
            return 0; // Success
        } catch (Exception e) {
            System.err.println("Action failed: " + e.getMessage());
            return 1; // Failure
        }
    }
    
    @Override
    public void printUsage() {
        System.out.println("Usage: astra-runner --action=" + getName() + " [options]");
        System.out.println("  --args='{\"someParam\":\"value\"}'");
    }
}
```

### 2. Spring Registration
Actions are automatically registered when annotated with `@Component`. The `ActionRegistry` will discover them via dependency injection.

### 3. Argument Handling
Actions receive arguments through:
- CLI parameters (--ga, --version, etc.)
- JSON arguments via --args parameter
- All arguments are merged into a single Map<String, String>

## Advanced Features

### JSON Output
Actions can support JSON output for programmatic consumption:

```java
if ("json".equals(args.get("format"))) {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> result = createResult();
    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
}
```

### Verbose Mode
Actions can provide detailed output when requested:

```java
boolean verbose = "true".equals(args.get("verbose"));
if (verbose) {
    // Show additional details
}
```

### Error Handling
Actions should handle errors gracefully:

```java
@Override
public int execute(Map<String, String> args) {
    try {
        // Action logic
        return 0;
    } catch (Exception e) {
        System.err.println("❌ Error: " + e.getMessage());
        return 1;
    }
}
```

## Examples

### System Information
```bash
# Basic system info
java -jar astra-runner.jar --action=system-info

# With additional arguments for context
java -jar astra-runner.jar --action=system-info --ga=com.example:test --version=1.0.0
```

### Argument Display
```bash
# Table format
java -jar astra-runner.jar --action=show-args --ga=com.example:test --version=1.0.0

# JSON format
java -jar astra-runner.jar --action=show-args --args='{"format":"json"}'

# Environment format for scripting
java -jar astra-runner.jar --action=show-args --args='{"format":"env"}' > env-vars.sh
source env-vars.sh
```

### Comprehensive Info
```bash
# Human readable
java -jar astra-runner.jar --action=info --ga=com.example:test

# JSON for programmatic use
java -jar astra-runner.jar --action=info --args='{"format":"json"}' > system-info.json
```

## Integration with CI/CD

Actions are particularly useful in CI/CD pipelines:

```yaml
# GitHub Actions example
- name: System Information
  run: java -jar astra-runner.jar --action=system-info

- name: Capture Environment
  run: |
    java -jar astra-runner.jar --action=show-args --args='{"format":"json"}' > build-env.json
    cat build-env.json
```

## Best Practices

1. **Consistent Output Format**: Use emojis and consistent formatting for human-readable output
2. **JSON Support**: Provide JSON output option for programmatic consumption
3. **Error Handling**: Return appropriate exit codes and error messages
4. **Documentation**: Implement comprehensive `printUsage()` methods
5. **Argument Validation**: Validate required arguments and provide clear error messages
6. **Logging**: Use appropriate log levels for different types of information

The action system provides a flexible, extensible way to add utility functions to the Astra Runner while maintaining clean separation of concerns and consistent user experience.