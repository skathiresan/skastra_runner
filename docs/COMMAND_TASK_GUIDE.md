# Creating Command-Based Tasks with Utilities

This guide shows how to quickly create new tasks that execute command line operations using the Astra Runner command execution utilities.

## Quick Start

### 1. Basic Shell Command Task

```java
@Component
public class ShellCommandTask implements Task {
    private static final Logger log = LoggerFactory.getLogger(ShellCommandTask.class);
    
    @Override
    public String getName() {
        return "shell-command";
    }
    
    @Override
    public TaskResult run(Map<String, String> args, Path outDir) throws Exception {
        String command = args.get("command");
        if (command == null) {
            return TaskResult.failure("No command specified");
        }
        
        CommandExecutor.CommandResult result = CommandExecutor.parse(command)
            .workingDirectory(args.getOrDefault("workingDir", "."))
            .timeout(Integer.parseInt(args.getOrDefault("timeout", "300")))
            .outputDirectory(outDir)
            .captureOutput(true)
            .execute();
        
        return TaskResultMapper.toTaskResult(result);
    }
}
```

### 2. Docker Command Task

```java
@Component
public class DockerTask implements Task {
    
    @Override
    public String getName() {
        return "docker-command";
    }
    
    @Override
    public TaskResult run(Map<String, String> args, Path outDir) throws Exception {
        String image = args.getOrDefault("image", "ubuntu:latest");
        String command = args.get("command");
        
        CommandBuilder.DockerCommandBuilder builder = CommandBuilder.docker()
            .run()
            .image(image)
            .removeAfter();
        
        // Add environment variables
        args.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("env."))
            .forEach(entry -> {
                String key = entry.getKey().substring(4); // Remove "env." prefix
                builder.env(key, entry.getValue());
            });
        
        CommandExecutor.CommandResult result = builder
            .args(command.split("\\s+"))
            .build()
            .outputDirectory(outDir)
            .timeout(Integer.parseInt(args.getOrDefault("timeout", "600")))
            .execute();
        
        return TaskResultMapper.toTaskResult(result);
    }
}
```

### 3. Database Query Task

```java
@Component
public class DatabaseQueryTask implements Task {
    
    @Override
    public String getName() {
        return "database-query";
    }
    
    @Override
    public TaskResult run(Map<String, String> args, Path outDir) throws Exception {
        String query = args.get("query");
        String dbType = args.getOrDefault("type", "mysql");
        
        CommandBuilder.DatabaseCommandBuilder builder = dbType.equals("postgresql") 
            ? CommandBuilder.postgresql() 
            : CommandBuilder.mysql();
        
        if (args.containsKey("host")) builder.host(args.get("host"));
        if (args.containsKey("port")) builder.port(Integer.parseInt(args.get("port")));
        if (args.containsKey("user")) builder.user(args.get("user"));
        if (args.containsKey("database")) builder.database(args.get("database"));
        if ("true".equals(args.get("password"))) builder.password();
        
        CommandExecutor.CommandResult result = builder
            .execute(query)
            .build()
            .outputDirectory(outDir)
            .timeout(Integer.parseInt(args.getOrDefault("timeout", "300")))
            .execute();
        
        return TaskResultMapper.toTaskResult(result);
    }
}
```

## Task Template

Here's a complete template for creating new command-based tasks:

```java
package com.example.mytask;

import com.acme.spi.Task;
import com.acme.spi.TaskResult;
import com.acme.astra.engine.util.CommandExecutor;
import com.acme.astra.engine.util.CommandBuilder;
import com.acme.astra.engine.util.TaskResultMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

@Component
public class MyCustomTask implements Task {
    private static final Logger log = LoggerFactory.getLogger(MyCustomTask.class);
    
    @Override
    public String getName() {
        return "my-custom-task";
    }
    
    @Override
    public String getDescription() {
        return "Description of what this task does";
    }
    
    @Override
    public TaskResult run(Map<String, String> args, Path outDir) throws Exception {
        log.info("Starting {} with args: {}", getName(), args);
        
        try {
            // 1. Extract and validate arguments
            String requiredArg = args.get("requiredArg");
            if (requiredArg == null) {
                return TaskResult.failure("Required argument 'requiredArg' not provided");
            }
            
            String optionalArg = args.getOrDefault("optionalArg", "defaultValue");
            int timeout = Integer.parseInt(args.getOrDefault("timeout", "300"));
            
            // 2. Build command using utilities
            CommandExecutor executor = buildCommand(args);
            
            // 3. Execute command
            CommandExecutor.CommandResult result = executor
                .workingDirectory(args.getOrDefault("workingDir", "."))
                .timeout(timeout)
                .outputDirectory(outDir)
                .captureOutput(true)
                .execute();
            
            // 4. Create additional files or metrics if needed
            Map<String, Object> additionalMetrics = createMetrics(result, args);
            List<String> additionalFiles = createAdditionalFiles(outDir, result);
            
            // 5. Convert to TaskResult
            return TaskResultMapper.toTaskResult(result, additionalFiles, additionalMetrics);
            
        } catch (Exception e) {
            log.error("Task execution failed", e);
            return TaskResult.failure("Task execution failed: " + e.getMessage())
                .withErrors(List.of(e.getMessage()));
        }
    }
    
    private CommandExecutor buildCommand(Map<String, String> args) {
        String commandType = args.getOrDefault("type", "shell");
        String command = args.get("command");
        
        return switch (commandType.toLowerCase()) {
            case "docker" -> buildDockerCommand(command, args);
            case "git" -> buildGitCommand(command, args);
            case "python" -> CommandBuilder.python(command);
            case "bash" -> CommandBuilder.bash(command);
            case "curl" -> buildHttpCommand(command, args);
            default -> CommandExecutor.parse(command);
        };
    }
    
    private CommandExecutor buildDockerCommand(String command, Map<String, String> args) {
        CommandBuilder.DockerCommandBuilder builder = CommandBuilder.docker().run();
        
        if (args.containsKey("image")) builder.image(args.get("image"));
        if ("true".equals(args.get("removeAfter"))) builder.removeAfter();
        if ("true".equals(args.get("interactive"))) builder.interactive();
        
        return builder.args(command.split("\\s+")).build();
    }
    
    private CommandExecutor buildGitCommand(String command, Map<String, String> args) {
        return CommandBuilder.git().args(command.split("\\s+")).build();
    }
    
    private CommandExecutor buildHttpCommand(String url, Map<String, String> args) {
        CommandBuilder.HttpCommandBuilder builder = CommandBuilder.curl().url(url);
        
        if (args.containsKey("method")) builder.method(args.get("method"));
        if (args.containsKey("data")) builder.data(args.get("data"));
        if (args.containsKey("json")) builder.json(args.get("json"));
        
        return builder.build();
    }
    
    private Map<String, Object> createMetrics(CommandExecutor.CommandResult result, Map<String, String> args) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("taskType", getName());
        metrics.put("argumentCount", args.size());
        // Add custom metrics specific to your task
        return metrics;
    }
    
    private List<String> createAdditionalFiles(Path outDir, CommandExecutor.CommandResult result) {
        // Create additional files like reports, summaries, etc.
        // Return list of file paths
        return List.of();
    }
}
```

## Registration

Don't forget to register your task in the SPI services file:

**File**: `src/main/resources/META-INF/services/com.acme.spi.Task`
```
com.example.mytask.MyCustomTask
```

## CLI Wrapper (Optional)

Create a CLI wrapper for standalone execution:

```java
public class MyCustomTaskCLI {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java -cp ... MyCustomTaskCLI \"<command>\" [options]");
            System.exit(1);
        }
        
        try {
            String command = args[0];
            Map<String, String> params = parseArgs(args);
            
            Map<String, String> taskArgs = new HashMap<>();
            taskArgs.put("command", command);
            taskArgs.putAll(params);
            
            Path outDir = Paths.get(params.getOrDefault("outputDir", "./output"));
            Files.createDirectories(outDir);
            
            MyCustomTask task = new MyCustomTask();
            TaskResult result = task.run(taskArgs, outDir);
            
            System.out.println("Status: " + result.getStatus());
            System.exit(result.getStatus() == TaskResult.Status.SUCCESS ? 0 : 1);
            
        } catch (Exception e) {
            System.err.println("Execution failed: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static Map<String, String> parseArgs(String[] args) {
        // Parse --key=value arguments
        Map<String, String> params = new HashMap<>();
        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("--") && args[i].contains("=")) {
                String[] parts = args[i].substring(2).split("=", 2);
                params.put(parts[0], parts[1]);
            }
        }
        return params;
    }
}
```

## Testing

Test your task with the utilities:

```java
@Test
void testTaskExecution() throws Exception {
    MyCustomTask task = new MyCustomTask();
    
    Map<String, String> args = Map.of(
        "command", "echo hello",
        "type", "shell",
        "timeout", "30"
    );
    
    Path tempDir = Files.createTempDirectory("test");
    TaskResult result = task.run(args, tempDir);
    
    assertThat(result.getStatus()).isEqualTo(TaskResult.Status.SUCCESS);
    assertThat(result.getOutputFiles()).isNotEmpty();
}
```

This template provides a solid foundation for creating new command-based tasks while leveraging all the utility features for consistent behavior and reduced code duplication.