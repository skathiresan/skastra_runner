# Command Execution Utilities

This directory contains reusable utilities for executing command line operations in Astra Runner tasks. These utilities provide a consistent, feature-rich API for command execution while eliminating code duplication across different task implementations.

## Utilities Overview

### 1. CommandExecutor
**Location**: `engine/src/main/java/com/acme/astra/engine/util/CommandExecutor.java`

Core utility for executing system commands with comprehensive control and output handling.

**Key Features**:
- Fluent API for command configuration
- Timeout handling with configurable limits
- Output capture to files (stdout/stderr)
- Working directory control
- Environment variable management
- Execution metrics and timing
- Process lifecycle management

**Basic Usage**:
```java
CommandExecutor.CommandResult result = CommandExecutor
    .command("ls", "-la")
    .workingDirectory("/tmp")
    .timeout(30)
    .captureOutput(true)
    .outputDirectory(outputDir)
    .execute();
```

**Parsing Commands**:
```java
// Parse string commands intelligently
CommandExecutor executor = CommandExecutor.parse("docker run ubuntu:latest echo hello");
CommandExecutor.CommandResult result = executor.execute();
```

### 2. TaskResultMapper
**Location**: `engine/src/main/java/com/acme/astra/engine/util/TaskResultMapper.java`

Utility to convert `CommandExecutor.CommandResult` objects to the standard `TaskResult` format required by the SPI contract.

**Usage**:
```java
CommandExecutor.CommandResult cmdResult = executor.execute();

// Basic conversion
TaskResult taskResult = TaskResultMapper.toTaskResult(cmdResult);

// With additional files and metrics
List<String> additionalFiles = List.of("config.json", "report.html");
Map<String, Object> additionalMetrics = Map.of("customMetric", 42);

TaskResult taskResult = TaskResultMapper.toTaskResult(
    cmdResult, 
    additionalFiles, 
    additionalMetrics
);
```

### 3. CommandBuilder
**Location**: `engine/src/main/java/com/acme/astra/engine/util/CommandBuilder.java`

Provides fluent builder patterns for common command types, eliminating the need to manually construct complex command lines.

**Supported Command Types**:

#### Shell Commands
```java
CommandExecutor executor = CommandBuilder.shell("ls -la");
CommandExecutor executor = CommandBuilder.bash("echo $HOME");
CommandExecutor executor = CommandBuilder.python("print('hello')");
CommandExecutor executor = CommandBuilder.powershell("Get-Process");
```

#### Docker Commands
```java
CommandExecutor executor = CommandBuilder.docker()
    .run()
    .image("python:3.9")
    .interactive()
    .removeAfter() 
    .env("PYTHONPATH", "/app")
    .volume("/host/data", "/container/data")
    .args("python", "script.py")
    .build();
```

#### Git Commands
```java
CommandExecutor executor = CommandBuilder.git()
    .clone("https://github.com/user/repo.git")
    .build();
    
CommandExecutor executor = CommandBuilder.git()
    .checkout("main")
    .build();
```

#### Database Commands
```java
// MySQL
CommandExecutor executor = CommandBuilder.mysql()
    .host("localhost")
    .port(3306)
    .user("root")
    .password() // Will prompt
    .database("mydb")
    .execute("SELECT * FROM users LIMIT 10")
    .build();

// PostgreSQL
CommandExecutor executor = CommandBuilder.postgresql()
    .host("localhost")
    .user("postgres")
    .database("mydb")
    .execute("SELECT version()")
    .build();
```

#### HTTP/curl Commands
```java
CommandExecutor executor = CommandBuilder.curl()
    .url("https://api.example.com/data")
    .method("POST")
    .json("{\"key\":\"value\"}")
    .header("Authorization: Bearer token")
    .header("Content-Type: application/json")
    .silent()
    .followRedirects()
    .build();
```

#### Build Tool Commands
```java
// Maven
CommandExecutor executor = CommandBuilder.maven("clean", "package", "-DskipTests");

// Gradle
CommandExecutor executor = CommandBuilder.gradle("build", "test");
```

## Integration Example

Here's how to use these utilities in a task implementation:

```java
@Component
public class MyCommandTask implements Task {
    
    @Override
    public TaskResult run(Map<String, String> args, Path outDir) throws Exception {
        String command = args.get("command");
        String commandType = args.getOrDefault("type", "shell");
        
        // Build command using utilities
        CommandExecutor executor = buildCommand(commandType, command, args);
        
        // Execute with configuration
        CommandExecutor.CommandResult result = executor
            .workingDirectory(args.getOrDefault("workingDir", "."))
            .timeout(Integer.parseInt(args.getOrDefault("timeout", "300")))
            .outputDirectory(outDir)
            .captureOutput(true)
            .execute();
        
        // Convert to TaskResult
        return TaskResultMapper.toTaskResult(result);
    }
    
    private CommandExecutor buildCommand(String type, String command, Map<String, String> args) {
        return switch (type.toLowerCase()) {
            case "docker" -> buildDockerCommand(command, args);
            case "git" -> CommandBuilder.git().args(command.split("\\s+")).build();
            case "python" -> CommandBuilder.python(command);
            case "mysql" -> buildMysqlCommand(command, args);
            default -> CommandExecutor.parse(command);
        };
    }
    
    private CommandExecutor buildDockerCommand(String command, Map<String, String> args) {
        CommandBuilder.DockerCommandBuilder builder = CommandBuilder.docker().run();
        
        if ("true".equals(args.get("removeAfter"))) builder.removeAfter();
        if (args.containsKey("image")) builder.image(args.get("image"));
        
        // Add environment variables
        args.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("env."))
            .forEach(entry -> {
                String key = entry.getKey().substring(4);
                builder.env(key, entry.getValue());
            });
        
        return builder.args(command.split("\\s+")).build();
    }
}
```

## Error Handling

All utilities provide comprehensive error handling:

```java
CommandExecutor.CommandResult result = executor.execute();

if (result.isSuccess()) {
    System.out.println("Command succeeded");
} else {
    System.out.println("Command failed with exit code: " + result.getExitCode());
    if (result.getErrorMessage() != null) {
        System.out.println("Error: " + result.getErrorMessage());
    }
}

// Check for timeout
if (result.isTimedOut()) {
    System.out.println("Command timed out after " + result.getDurationMs() + "ms");
}
```

## Output Handling

Commands can capture output to files automatically:

```java
CommandExecutor.CommandResult result = executor
    .outputDirectory(outputDir)
    .captureOutput(true)
    .execute();

// Access output files
Path stdoutFile = result.getStdoutFile();
Path stderrFile = result.getStderrFile();

if (stdoutFile != null && Files.exists(stdoutFile)) {
    String output = Files.readString(stdoutFile);
    System.out.println("Command output: " + output);
}
```

## Testing

These utilities are designed to be easily testable:

```java
@Test
void testCommandExecution() {
    CommandExecutor.CommandResult result = CommandExecutor
        .command("echo", "hello")
        .execute();
    
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getExitCode()).isEqualTo(0);
    assertThat(result.getDurationMs()).isGreaterThan(0);
}
```

## Best Practices

1. **Always set timeouts** for long-running commands to prevent hanging
2. **Use CommandBuilder** for complex commands instead of string concatenation
3. **Capture output** when you need to process command results
4. **Handle errors gracefully** using the result status and error information
5. **Use specific command types** (docker, git, etc.) for better validation and features
6. **Pass through arguments** from task parameters to command builders for flexibility

## Thread Safety

All utilities are thread-safe and can be used concurrently from multiple tasks. Each execution creates its own process and manages its own resources.

## Performance Considerations

- Command execution is synchronous by design for predictable behavior
- Output capture adds minimal overhead but can consume disk space for large outputs
- Process creation has inherent OS overhead - consider reusing connections for database commands
- Timeout handling uses minimal CPU resources with efficient polling

## Examples Directory

See the `examples/my-command-job` directory for a complete working example that demonstrates:
- Using all utility types
- CLI interface with help and examples
- Parameter passing and configuration
- Error handling and reporting
- Integration with the Astra Runner framework