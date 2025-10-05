package com.example.myjob;

import com.acme.spi.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Command-line interface for MyCommandJobTask.
 * This demonstrates how to use the task with command execution utilities.
 * 
 * Usage examples:
 * - Simple shell command: java -cp ... com.example.myjob.MyCommandJobCLI "ls -la"
 * - Docker command: java -cp ... com.example.myjob.MyCommandJobCLI "echo hello" --type=docker --image=ubuntu:latest
 * - Git command: java -cp ... com.example.myjob.MyCommandJobCLI "git status" --type=git
 * - With options: java -cp ... com.example.myjob.MyCommandJobCLI "python --version" --type=python --timeout=60
 */
public class MyCommandJobCLI {
    
    private static final Logger log = LoggerFactory.getLogger(MyCommandJobCLI.class);
    
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        
        // Handle help request
        if (args[0].equals("--help") || args[0].equals("-h")) {
            printUsage();
            showExamples();
            System.exit(0);
        }
        
        try {
            String command = args[0];
            Map<String, String> params = parseArgs(args);
            
            // Set up task arguments
            Map<String, String> taskArgs = new HashMap<>();
            taskArgs.put("command", command);
            taskArgs.put("workingDir", params.getOrDefault("workingDir", "."));
            taskArgs.put("timeout", params.getOrDefault("timeout", "300"));
            taskArgs.put("type", params.getOrDefault("type", "auto"));
            
            // Add all additional parameters for specialized commands
            params.forEach((key, value) -> {
                if (!key.equals("outputDir") && !key.equals("workingDir") && !key.equals("timeout") && !key.equals("type")) {
                    taskArgs.put(key, value);
                }
            });
            
            log.info("Executing command with type '{}': {}", taskArgs.get("type"), command);
            
            // Create output directory
            String outputDir = params.getOrDefault("outputDir", "./output/my-command-job-" + System.currentTimeMillis());
            Path outDir = Paths.get(outputDir);
            Files.createDirectories(outDir);
            
            // Create and run the task
            MyCommandJobTask task = new MyCommandJobTask();
            TaskResult result = task.run(taskArgs, outDir);
            
            // Print results
            System.out.println("✅ Task completed with status: " + result.getStatus());
            System.out.println("📁 Output directory: " + outDir.toAbsolutePath());
            
            if (result.getStatus() == TaskResult.Status.SUCCESS) {
                System.out.println("🎉 Command executed successfully");
            } else {
                System.out.println("❌ Command failed");
                if (result.getMessage() != null) {
                    System.out.println("💥 Error: " + result.getMessage());
                }
            }
            
            // Show timing information
            if (result.getStartTime() != null && result.getEndTime() != null) {
                long durationMs = result.getEndTime().toEpochMilli() - result.getStartTime().toEpochMilli();
                System.out.println("⏱️  Execution time: " + durationMs + " ms");
            }
            
            // Show output files
            if (!result.getOutputFiles().isEmpty()) {
                System.out.println("\n📄 Output files:");
                result.getOutputFiles().forEach(file -> System.out.println("   📝 " + file));
            }
            
            // Show metrics
            if (result.getMetrics() != null && !result.getMetrics().isEmpty()) {
                System.out.println("\n📊 Metrics:");
                result.getMetrics().forEach((key, value) -> 
                    System.out.println("   " + key + ": " + value));
            }
            
            // Show errors if any
            if (result.getErrors() != null && !result.getErrors().isEmpty()) {
                System.out.println("\n🚨 Errors:");
                result.getErrors().forEach(error -> System.out.println("   ⚠️  " + error));
            }
            
            System.exit(result.getStatus() == TaskResult.Status.SUCCESS ? 0 : 1);
            
        } catch (Exception e) {
            log.error("CLI execution failed", e);
            System.err.println("💥 CLI execution failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.out.println("MyCommandJobTask CLI - Execute commands with advanced utilities");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -cp ... com.example.myjob.MyCommandJobCLI \"<command>\" [options]");
        System.out.println();
        System.out.println("Basic Options:");
        System.out.println("  --type=<type>         Command type (auto, shell, bash, python, docker, git, curl, mysql, postgresql, maven, gradle)");
        System.out.println("  --workingDir=<dir>    Working directory for command execution (default: .)");
        System.out.println("  --outputDir=<dir>     Output directory for results (default: ./output/my-command-job-<timestamp>)");
        System.out.println("  --timeout=<seconds>   Timeout in seconds (default: 300)");
        System.out.println();
        System.out.println("Docker Options:");
        System.out.println("  --image=<image>       Docker image to use");
        System.out.println("  --removeAfter=true    Remove container after execution");
        System.out.println("  --interactive=true    Run in interactive mode");
        System.out.println("  --env.<key>=<value>   Environment variables");
        System.out.println("  --volume.<src>=<dst>  Volume mounts");
        System.out.println();
        System.out.println("Database Options:");
        System.out.println("  --host=<host>         Database host");
        System.out.println("  --port=<port>         Database port");
        System.out.println("  --user=<user>         Database user");
        System.out.println("  --password=true       Prompt for password");
        System.out.println("  --database=<db>       Database name");
        System.out.println();
        System.out.println("HTTP Options:");
        System.out.println("  --method=<method>     HTTP method (GET, POST, PUT, DELETE)");
        System.out.println("  --data=<data>         Request body data");
        System.out.println("  --json=<json>         JSON request body");
        System.out.println("  --header.<name>=<val> HTTP headers");
        System.out.println("  --silent=true         Silent mode");
        System.out.println("  --followRedirects=true Follow redirects");
        System.out.println("  --insecure=true       Allow insecure connections");
    }
    
    /**
     * Show usage examples for different command types.
     */
    public static void showExamples() {
        System.out.println();
        System.out.println("Examples:");
        System.out.println();
        
        System.out.println("Shell commands:");
        System.out.println("  java -cp ... com.example.myjob.MyCommandJobCLI \"ls -la\"");
        System.out.println("  java -cp ... com.example.myjob.MyCommandJobCLI \"echo 'Hello World'\" --type=bash");
        System.out.println();
        
        System.out.println("Docker commands:");
        System.out.println("  java -cp ... com.example.myjob.MyCommandJobCLI \"echo hello\" --type=docker --image=ubuntu:latest");
        System.out.println("  java -cp ... com.example.myjob.MyCommandJobCLI \"python -c 'print(\\\"hello\\\"))'\" --type=docker --image=python:3.9 --env.PYTHONPATH=/app");
        System.out.println();
        
        System.out.println("Git commands:");
        System.out.println("  java -cp ... com.example.myjob.MyCommandJobCLI \"git status\" --type=git");
        System.out.println("  java -cp ... com.example.myjob.MyCommandJobCLI \"git clone https://github.com/user/repo.git\" --type=git");
        System.out.println();
        
        System.out.println("Python commands:");
        System.out.println("  java -cp ... com.example.myjob.MyCommandJobCLI \"print('Hello from Python')\" --type=python");
        System.out.println("  java -cp ... com.example.myjob.MyCommandJobCLI \"-c 'import sys; print(sys.version)'\" --type=python");
        System.out.println();
        
        System.out.println("HTTP/curl commands:");
        System.out.println("  java -cp ... com.example.myjob.MyCommandJobCLI \"https://api.github.com/users/octocat\" --type=curl");
        System.out.println("  java -cp ... com.example.myjob.MyCommandJobCLI \"https://api.example.com/data\" --type=curl --method=POST --json='{\\\"key\\\":\\\"value\\\"}' --header.Authorization='Bearer token'");
        System.out.println();
        
        System.out.println("Database commands:");
        System.out.println("  java -cp ... com.example.myjob.MyCommandJobCLI \"SELECT * FROM users LIMIT 5\" --type=mysql --host=localhost --user=root --database=mydb");
        System.out.println("  java -cp ... com.example.myjob.MyCommandJobCLI \"SELECT version()\" --type=postgresql --host=localhost --user=postgres");
        System.out.println();
        
        System.out.println("Build tool commands:");
        System.out.println("  java -cp ... com.example.myjob.MyCommandJobCLI \"clean package\" --type=maven");
        System.out.println("  java -cp ... com.example.myjob.MyCommandJobCLI \"build test\" --type=gradle");
    }
    
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> params = new HashMap<>();
        
        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                String arg = args[i].substring(2);
                if (arg.contains("=")) {
                    String[] parts = arg.split("=", 2);
                    params.put(parts[0], parts[1]);
                } else if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    params.put(arg, args[i + 1]);
                    i++; // Skip the value argument
                } else {
                    params.put(arg, "true"); // Boolean flag
                }
            }
        }
        
        return params;
    }
}