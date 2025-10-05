package com.acme.astra.app.cli.action;

import com.acme.astra.engine.resolver.ArtifactResolver;
import com.acme.astra.engine.util.CommandExecutor;
import com.acme.astra.engine.util.TaskResultMapper;
import com.acme.astra.model.RunSummary.ResolvedArtifact;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Action that downloads JARs from Artifactory and executes them with predefined parameters.
 * This action demonstrates integration with existing Artifactory resolution and command execution utilities.
 */
@Component
public class TriggerJobAction implements CliAction {
    
    private static final Logger log = LoggerFactory.getLogger(TriggerJobAction.class);
    
    private final ArtifactResolver artifactResolver;
    private final ObjectMapper objectMapper;
    
    // Default JARs to download and execute (can be overridden via arguments)
    private static final List<JobConfig> DEFAULT_JOBS = List.of(
        new JobConfig("com.example:data-processor", "latest.release", 
                     List.of("--mode=batch", "--input=/data/input", "--output=/data/output")),
        new JobConfig("com.example:notification-service", "1.0.0", 
                     List.of("--type=email", "--template=job-completion")),
        new JobConfig("com.example:cleanup-utility", "latest.release", 
                     List.of("--target=/tmp", "--age=7d", "--dry-run=false"))
    );
    
    @Autowired
    public TriggerJobAction(ArtifactResolver artifactResolver) {
        this.artifactResolver = artifactResolver;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getName() {
        return "trigger-job";
    }
    
    @Override
    public String getDescription() {
        return "Download JARs from Artifactory and execute them with predefined parameters";
    }
    
    @Override
    public int execute(Map<String, String> args) {
        try {
            Instant startTime = Instant.now();
            System.out.println("=== Trigger Job Action ===");
            System.out.println("Started at: " + startTime);
            System.out.println();
            
            // Parse configuration
            List<JobConfig> jobs = parseJobConfiguration(args);
            boolean parallel = "true".equals(args.get("parallel"));
            boolean dryRun = "true".equals(args.get("dryRun"));
            String outputFormat = args.getOrDefault("format", "human");
            
            // Create workspace
            Path workspace = createWorkspace(args);
            System.out.println("📁 Workspace: " + workspace.toAbsolutePath());
            System.out.println("🔧 Jobs to execute: " + jobs.size());
            System.out.println("⚡ Parallel execution: " + parallel);
            System.out.println("🧪 Dry run mode: " + dryRun);
            System.out.println();
            
            // Show all available arguments for reference
            if ("true".equals(args.get("showArgs"))) {
                printArgumentsInfo(args);
            }
            
            // Execute jobs
            List<JobResult> results = parallel ? 
                executeJobsParallel(jobs, workspace, args, dryRun) : 
                executeJobsSequential(jobs, workspace, args, dryRun);
            
            // Generate results
            return generateResults(results, outputFormat, workspace, startTime);
            
        } catch (Exception e) {
            log.error("Trigger job action failed", e);
            System.err.println("❌ Trigger job action failed: " + e.getMessage());
            return 1;
        }
    }
    
    @Override
    public void printUsage() {
        System.out.println("Usage: astra-runner --action=" + getName() + " [options]");
        System.out.println();
        System.out.println("Description:");
        System.out.println("  " + getDescription());
        System.out.println();
        System.out.println("JSON Arguments (via --args):");
        System.out.println("  {");
        System.out.println("    \"parallel\": \"true|false\",      // Execute jobs in parallel (default: false)");
        System.out.println("    \"dryRun\": \"true|false\",        // Download JARs but don't execute (default: false)");
        System.out.println("    \"format\": \"human|json\",        // Output format (default: human)");
        System.out.println("    \"workspace\": \"/path/to/dir\",    // Custom workspace directory");
        System.out.println("    \"timeout\": \"300\",              // Timeout per job in seconds (default: 300)");
        System.out.println("    \"showArgs\": \"true|false\",      // Display all available arguments");
        System.out.println("    \"jobs\": [                       // Custom job configuration (optional)");
        System.out.println("      {");
        System.out.println("        \"groupArtifact\": \"com.example:my-job\",");
        System.out.println("        \"version\": \"1.0.0\",");
        System.out.println("        \"args\": [\"--param1=value1\", \"--param2=value2\"]");
        System.out.println("      }");
        System.out.println("    ]");
        System.out.println("  }");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Execute default jobs sequentially");
        System.out.println("  astra-runner --action=" + getName());
        System.out.println();
        System.out.println("  # Execute default jobs in parallel");
        System.out.println("  astra-runner --action=" + getName() + " --args='{\"parallel\":\"true\"}'");
        System.out.println();
        System.out.println("  # Dry run to test downloads");
        System.out.println("  astra-runner --action=" + getName() + " --args='{\"dryRun\":\"true\"}'");
        System.out.println();
        System.out.println("  # Custom job configuration");
        System.out.println("  astra-runner --action=" + getName() + " --args='{");
        System.out.println("    \"jobs\":[{");
        System.out.println("      \"groupArtifact\":\"com.custom:job\",");
        System.out.println("      \"version\":\"2.0.0\",");
        System.out.println("      \"args\":[\"--config=/path/to/config\"]");
        System.out.println("    }]");
        System.out.println("  }'");
        System.out.println();
        System.out.println("  # JSON output for programmatic use");
        System.out.println("  astra-runner --action=" + getName() + " --args='{\"format\":\"json\"}'");
    }
    
    private List<JobConfig> parseJobConfiguration(Map<String, String> args) throws Exception {
        String jobsJson = args.get("jobs");
        if (jobsJson == null) {
            return DEFAULT_JOBS;
        }
        
        // Parse custom job configuration
        JobConfig[] jobArray = objectMapper.readValue(jobsJson, JobConfig[].class);
        return List.of(jobArray);
    }
    
    private Path createWorkspace(Map<String, String> args) throws Exception {
        String workspaceStr = args.get("workspace");
        Path workspace = workspaceStr != null ? 
            Paths.get(workspaceStr) : 
            Paths.get("./workspace/trigger-job-" + System.currentTimeMillis());
        
        Files.createDirectories(workspace);
        return workspace;
    }
    
    private void printArgumentsInfo(Map<String, String> args) {
        System.out.println("📋 Available Arguments (from Astra Runner):");
        if (args.isEmpty()) {
            System.out.println("   No arguments available");
        } else {
            args.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> 
                        System.out.println("   " + entry.getKey() + " = " + entry.getValue()));
        }
        System.out.println();
    }
    
    private List<JobResult> executeJobsSequential(
            List<JobConfig> jobs, Path workspace, Map<String, String> args, boolean dryRun) {
        
        List<JobResult> results = new ArrayList<>();
        
        for (int i = 0; i < jobs.size(); i++) {
            JobConfig job = jobs.get(i);
            System.out.println("🚀 Executing Job " + (i + 1) + "/" + jobs.size() + ": " + job.groupArtifact);
            
            JobResult result = executeJob(job, workspace, args, dryRun);
            results.add(result);
            
            if (!result.success && "true".equals(args.get("stopOnFailure"))) {
                System.out.println("❌ Stopping execution due to job failure");
                break;
            }
        }
        
        return results;
    }
    
    private List<JobResult> executeJobsParallel(
            List<JobConfig> jobs, Path workspace, Map<String, String> args, boolean dryRun) {
        
        System.out.println("⚡ Executing jobs in parallel...");
        
        ExecutorService executor = Executors.newFixedThreadPool(
            Math.min(jobs.size(), Integer.parseInt(args.getOrDefault("maxParallel", "3"))));
        
        try {
            List<CompletableFuture<JobResult>> futures = new ArrayList<>();
            
            for (JobConfig job : jobs) {
                CompletableFuture<JobResult> future = CompletableFuture.supplyAsync(
                    () -> executeJob(job, workspace, args, dryRun), executor);
                futures.add(future);
            }
            
            // Wait for all jobs to complete
            return futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
                    
        } finally {
            executor.shutdown();
        }
    }
    
    private JobResult executeJob(JobConfig job, Path workspace, Map<String, String> args, boolean dryRun) {
        Instant startTime = Instant.now();
        
        try {
            // Create job-specific workspace
            Path jobWorkspace = workspace.resolve("job-" + sanitizeJobName(job.groupArtifact));
            Files.createDirectories(jobWorkspace);
            
            // 1. Download JAR from Artifactory
            System.out.println("📥 Downloading " + job.groupArtifact + ":" + job.version);
            ResolvedArtifact artifact = artifactResolver.resolve(job.groupArtifact, job.version, jobWorkspace);
            
            if (dryRun) {
                System.out.println("🧪 Dry run - would execute: java -jar " + artifact.getPath() + " " + String.join(" ", job.args));
                return new JobResult(job, true, 0, "Dry run successful", 
                                   startTime, Instant.now(), artifact.getPath());
            }
            
            // 2. Execute JAR with predefined parameters
            Path jarPath = Path.of(artifact.getPath());
            System.out.println("⚡ Executing JAR: " + jarPath.getFileName());
            
            List<String> command = new ArrayList<>();
            command.add("java");
            command.add("-jar");
            command.add(artifact.getPath());
            command.addAll(job.args);
            
            // Add any additional JVM args from the parent application
            String jvmArgs = args.get("jvmArgs");
            if (jvmArgs != null) {
                String[] jvmArgArray = jvmArgs.split(",");
                for (int i = jvmArgArray.length - 1; i >= 0; i--) {
                    command.add(2, jvmArgArray[i].trim()); // Insert after java but before -jar
                }
            }
            
            int timeout = Integer.parseInt(args.getOrDefault("timeout", "300"));
            
            CommandExecutor.CommandResult result = CommandExecutor.of(command)
                    .workingDirectory(jobWorkspace)
                    .timeout(timeout)
                    .outputDirectory(jobWorkspace)
                    .captureOutput(true)
                    .execute();
            
            boolean success = result.isSuccess();
            String message = success ? "Job completed successfully" : 
                           "Job failed with exit code " + result.getExitCode();
            
            if (!success && result.getErrorMessage() != null) {
                message += ": " + result.getErrorMessage();
            }
            
            System.out.println(success ? "✅ " + job.groupArtifact + " completed" : 
                                       "❌ " + job.groupArtifact + " failed");
            
            return new JobResult(job, success, result.getExitCode(), message, 
                               startTime, Instant.now(), artifact.getPath());
            
        } catch (Exception e) {
            log.error("Job execution failed: " + job.groupArtifact, e);
            System.out.println("❌ " + job.groupArtifact + " failed: " + e.getMessage());
            
            return new JobResult(job, false, -1, "Execution failed: " + e.getMessage(), 
                               startTime, Instant.now(), null);
        }
    }
    
    private int generateResults(List<JobResult> results, String format, Path workspace, Instant startTime) throws Exception {
        Instant endTime = Instant.now();
        long totalDuration = endTime.toEpochMilli() - startTime.toEpochMilli();
        
        int successCount = (int) results.stream().mapToInt(r -> r.success ? 1 : 0).sum();
        int failureCount = results.size() - successCount;
        
        if ("json".equals(format)) {
            generateJsonResults(results, workspace, startTime, endTime, totalDuration);
        } else {
            generateHumanResults(results, successCount, failureCount, totalDuration);
        }
        
        // Overall success if all jobs succeeded
        return failureCount == 0 ? 0 : 1;
    }
    
    private void generateHumanResults(List<JobResult> results, int successCount, int failureCount, long totalDuration) {
        System.out.println();
        System.out.println("📊 Execution Summary:");
        System.out.println("   Total Jobs: " + results.size());
        System.out.println("   Successful: " + successCount);
        System.out.println("   Failed: " + failureCount);
        System.out.println("   Total Duration: " + totalDuration + "ms");
        System.out.println();
        
        System.out.println("📋 Job Results:");
        for (int i = 0; i < results.size(); i++) {
            JobResult result = results.get(i);
            String status = result.success ? "✅ SUCCESS" : "❌ FAILED";
            long duration = result.endTime.toEpochMilli() - result.startTime.toEpochMilli();
            
            System.out.println("   " + (i + 1) + ". " + result.job.groupArtifact + " - " + status);
            System.out.println("      Duration: " + duration + "ms");
            System.out.println("      Exit Code: " + result.exitCode);
            System.out.println("      Message: " + result.message);
            if (result.jarPath != null) {
                System.out.println("      JAR: " + result.jarPath);
            }
            System.out.println();
        }
    }
    
    private void generateJsonResults(List<JobResult> results, Path workspace, Instant startTime, Instant endTime, long totalDuration) throws Exception {
        Map<String, Object> summary = new HashMap<>();
        summary.put("startTime", startTime.toString());
        summary.put("endTime", endTime.toString());
        summary.put("totalDurationMs", totalDuration);
        summary.put("totalJobs", results.size());
        summary.put("successfulJobs", results.stream().mapToInt(r -> r.success ? 1 : 0).sum());
        summary.put("failedJobs", results.stream().mapToInt(r -> r.success ? 0 : 1).sum());
        summary.put("workspace", workspace.toAbsolutePath().toString());
        
        List<Map<String, Object>> jobResults = new ArrayList<>();
        for (JobResult result : results) {
            Map<String, Object> jobResult = new HashMap<>();
            jobResult.put("groupArtifact", result.job.groupArtifact);
            jobResult.put("version", result.job.version);
            jobResult.put("args", result.job.args);
            jobResult.put("success", result.success);
            jobResult.put("exitCode", result.exitCode);
            jobResult.put("message", result.message);
            jobResult.put("startTime", result.startTime.toString());
            jobResult.put("endTime", result.endTime.toString());
            jobResult.put("durationMs", result.endTime.toEpochMilli() - result.startTime.toEpochMilli());
            if (result.jarPath != null) {
                jobResult.put("jarPath", result.jarPath);
            }
            jobResults.add(jobResult);
        }
        
        Map<String, Object> output = new HashMap<>();
        output.put("summary", summary);
        output.put("results", jobResults);
        
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(output));
    }
    
    private String sanitizeJobName(String groupArtifact) {
        return groupArtifact.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
    
    // Inner classes for configuration and results
    public static class JobConfig {
        public String groupArtifact;
        public String version;
        public List<String> args;
        
        public JobConfig() {} // For JSON deserialization
        
        public JobConfig(String groupArtifact, String version, List<String> args) {
            this.groupArtifact = groupArtifact;
            this.version = version;
            this.args = args;
        }
    }
    
    private static class JobResult {
        final JobConfig job;
        final boolean success;
        final int exitCode;
        final String message;
        final Instant startTime;
        final Instant endTime;
        final String jarPath;
        
        JobResult(JobConfig job, boolean success, int exitCode, String message, 
                 Instant startTime, Instant endTime, String jarPath) {
            this.job = job;
            this.success = success;
            this.exitCode = exitCode;
            this.message = message;
            this.startTime = startTime;
            this.endTime = endTime;
            this.jarPath = jarPath;
        }
    }
}