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