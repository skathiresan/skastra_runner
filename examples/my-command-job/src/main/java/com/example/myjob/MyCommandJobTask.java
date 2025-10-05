package com.example.myjob;

import com.acme.spi.Task;
import com.acme.spi.TaskResult;
import com.acme.astra.engine.util.CommandExecutor;
import com.acme.astra.engine.util.CommandBuilder;
import com.acme.astra.engine.util.TaskResultMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example Task that executes command line jobs.
 * This demonstrates how to create a task that runs system commands.
 */
public class MyCommandJobTask implements Task {
    
    private static final Logger log = LoggerFactory.getLogger(MyCommandJobTask.class);
    
    @Override
    public TaskResult run(Map<String, String> args, Path outDir) throws Exception {
        log.info("MyCommandJobTask started with args: {}", args);
        
        try {
            // Get command from arguments
            String command = args.get("command");
            if (command == null || command.trim().isEmpty()) {
                return TaskResult.failure("No command specified. Use 'command' parameter.");
            }
            
            // Get other parameters
            String workingDir = args.getOrDefault("workingDir", ".");
            int timeoutSeconds = Integer.parseInt(args.getOrDefault("timeout", "300"));
            String commandType = args.getOrDefault("type", "shell"); // shell, bash, python, docker, etc.
            
            log.info("Executing {} command: {}", commandType, command);
            log.info("Working directory: {}", workingDir);
            log.info("Timeout: {} seconds", timeoutSeconds);
            
            // Build the command using the utilities
            CommandExecutor executor = buildCommand(commandType, command, args);
            
            // Configure execution
            CommandExecutor.CommandResult result = executor
                .workingDirectory(workingDir)
                .timeout(timeoutSeconds)
                .outputDirectory(outDir)
                .captureOutput(true)
                .execute();
            
            // Create execution report
            Path reportFile = outDir.resolve("execution-report.json");
            Map<String, Object> report = createExecutionReport(result, args);
            
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(reportFile.toFile(), report);
            
            // Convert to TaskResult using the utility
            Map<String, Object> additionalMetrics = new HashMap<>();
            additionalMetrics.put("commandType", commandType);
            additionalMetrics.put("argumentsCount", args.size());
            additionalMetrics.put("reportFile", reportFile.toString());
            
            List<String> additionalFiles = List.of(reportFile.toString());
            
            return TaskResultMapper.toTaskResult(result, additionalFiles, additionalMetrics);
                
        } catch (Exception e) {
            log.error("Command execution failed", e);
            return TaskResult.failure("Command execution failed: " + e.getMessage())
                .withTiming(Instant.now(), Instant.now())
                .withErrors(List.of(e.getMessage()));
        }
    }
    
    /**
     * Build the appropriate command executor based on the command type.
     */
    private CommandExecutor buildCommand(String commandType, String command, Map<String, String> args) {
        return switch (commandType.toLowerCase()) {
            case "bash" -> CommandBuilder.bash(command);
            case "shell", "sh" -> CommandBuilder.shell(command);
            case "python" -> CommandBuilder.python(command);
            case "powershell", "ps" -> CommandBuilder.powershell(command);
            case "docker" -> buildDockerCommand(command, args);
            case "mysql" -> buildMysqlCommand(command, args);
            case "postgresql", "psql" -> buildPostgreSQLCommand(command, args);
            case "curl", "http" -> buildHttpCommand(command, args);
            case "git" -> buildGitCommand(command, args);
            case "maven", "mvn" -> CommandBuilder.maven(command.split("\\s+"));
            case "gradle" -> CommandBuilder.gradle(command.split("\\s+"));
            default -> CommandExecutor.parse(command); // Generic command parsing
        };
    }
    
    /**
     * Build Docker command with additional parameters.
     */
    private CommandExecutor buildDockerCommand(String command, Map<String, String> args) {
        CommandBuilder.DockerCommandBuilder builder = CommandBuilder.docker().run();
        
        if ("true".equals(args.get("removeAfter"))) {
            builder.removeAfter();
        }
        if ("true".equals(args.get("interactive"))) {
            builder.interactive();
        }
        
        // Add environment variables
        args.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("env."))
            .forEach(entry -> {
                String envKey = entry.getKey().substring(4); // Remove "env." prefix
                builder.env(envKey, entry.getValue());
            });
        
        // Add volumes
        args.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("volume."))
            .forEach(entry -> {
                String[] paths = entry.getValue().split(":");
                if (paths.length == 2) {
                    builder.volume(paths[0], paths[1]);
                }
            });
        
        return builder.image(args.getOrDefault("image", "ubuntu:latest"))
                     .args(command.split("\\s+"))
                     .build();
    }
    
    /**
     * Build MySQL command with connection parameters.
     */
    private CommandExecutor buildMysqlCommand(String command, Map<String, String> args) {
        CommandBuilder.DatabaseCommandBuilder builder = CommandBuilder.mysql();
        
        if (args.containsKey("host")) builder.host(args.get("host"));
        if (args.containsKey("port")) builder.port(Integer.parseInt(args.get("port")));
        if (args.containsKey("user")) builder.user(args.get("user"));
        if ("true".equals(args.get("password"))) builder.password();
        if (args.containsKey("database")) builder.database(args.get("database"));
        
        return builder.execute(command).build();
    }
    
    /**
     * Build PostgreSQL command with connection parameters.
     */
    private CommandExecutor buildPostgreSQLCommand(String command, Map<String, String> args) {
        CommandBuilder.DatabaseCommandBuilder builder = CommandBuilder.postgresql();
        
        if (args.containsKey("host")) builder.host(args.get("host"));
        if (args.containsKey("port")) builder.port(Integer.parseInt(args.get("port")));
        if (args.containsKey("user")) builder.user(args.get("user"));
        if (args.containsKey("database")) builder.database(args.get("database"));
        
        return builder.execute(command).build();
    }
    
    /**
     * Build HTTP/curl command with additional parameters.
     */
    private CommandExecutor buildHttpCommand(String url, Map<String, String> args) {
        CommandBuilder.HttpCommandBuilder builder = CommandBuilder.curl().url(url);
        
        if (args.containsKey("method")) builder.method(args.get("method"));
        if (args.containsKey("data")) builder.data(args.get("data"));
        if (args.containsKey("json")) builder.json(args.get("json"));
        if ("true".equals(args.get("silent"))) builder.silent();
        if ("true".equals(args.get("followRedirects"))) builder.followRedirects();
        if ("true".equals(args.get("insecure"))) builder.insecure();
        
        // Add headers
        args.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("header."))
            .forEach(entry -> builder.header(entry.getValue()));
        
        return builder.build();
    }
    
    /**
     * Build Git command with additional parameters.
     */
    private CommandExecutor buildGitCommand(String command, Map<String, String> args) {
        String[] parts = command.split("\\s+");
        CommandBuilder.GitCommandBuilder builder = CommandBuilder.git();
        
        if (parts.length > 0) {
            switch (parts[0]) {
                case "clone" -> {
                    if (parts.length > 1) builder.clone(parts[1]);
                }
                case "pull" -> builder.pull();
                case "push" -> builder.push();
                case "status" -> builder.status();
                case "checkout" -> {
                    if (parts.length > 1) builder.checkout(parts[1]);
                }
                default -> builder.args(parts);
            }
        }
        
        return builder.build();
    }
    
    /**
     * Create detailed execution report.
     */
    private Map<String, Object> createExecutionReport(CommandExecutor.CommandResult result, Map<String, String> args) {
        Map<String, Object> report = new HashMap<>();
        report.put("command", String.join(" ", result.getCommand()));
        report.put("workingDirectory", result.getWorkingDirectory() != null ? result.getWorkingDirectory().toString() : null);
        report.put("exitCode", result.getExitCode());
        report.put("success", result.isSuccess());
        report.put("timedOut", result.isTimedOut());
        report.put("durationMs", result.getDurationMs());
        report.put("startTime", result.getStartTime().toString());
        report.put("endTime", result.getEndTime().toString());
        report.put("arguments", args);
        report.put("stdoutFile", result.getStdoutFile() != null ? result.getStdoutFile().toString() : null);
        report.put("stderrFile", result.getStderrFile() != null ? result.getStderrFile().toString() : null);
        
        if (result.getErrorMessage() != null) {
            report.put("errorMessage", result.getErrorMessage());
        }
        
        return report;
    }
    
    @Override
    public String getName() {
        return "MyCommandJobTask";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
}