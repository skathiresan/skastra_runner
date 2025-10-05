package com.acme.astra.engine.executor;

import com.acme.astra.model.ExecutionConfig;
import com.acme.astra.model.RunSummary.ResolvedArtifact;
import com.acme.spi.Task;
import com.acme.spi.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Executes resolved artifacts in either CLI or SPI mode.
 */
@Component
public class ArtifactExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(ArtifactExecutor.class);
    
    /**
     * Execute artifact with given configuration.
     */
    public TaskResult execute(ResolvedArtifact artifact, ExecutionConfig config, Path reportsDir) throws Exception {
        log.info("Executing artifact {} in {} mode", artifact.getArtifactId(), config.getMode());
        
        Instant startTime = Instant.now();
        
        try {
            TaskResult result = switch (config.getMode()) {
                case CLI -> executeCli(artifact, config, reportsDir);
                case SPI -> executeSpi(artifact, config, reportsDir);
            };
            
            Instant endTime = Instant.now();
            result.setStartTime(startTime);
            result.setEndTime(endTime);
            result.setDurationMs(endTime.toEpochMilli() - startTime.toEpochMilli());
            
            return result;
        } catch (Exception e) {
            Instant endTime = Instant.now();
            TaskResult result = TaskResult.failure("Execution failed: " + e.getMessage());
            result.setStartTime(startTime);
            result.setEndTime(endTime);
            result.setDurationMs(endTime.toEpochMilli() - startTime.toEpochMilli());
            result.setErrors(List.of(e.getMessage()));
            return result;
        }
    }
    
    private TaskResult executeCli(ResolvedArtifact artifact, ExecutionConfig config, Path reportsDir) throws Exception {
        log.info("Executing CLI mode for {}", artifact.getPath());
        
        List<String> command = new ArrayList<>();
        command.add("java");
        
        // Add JVM arguments if specified
        if (config.getJvmArgs() != null) {
            command.addAll(config.getJvmArgs());
        }
        
        command.add("-jar");
        command.add(artifact.getPath());
        
        // Add runtime arguments
        if (config.getArguments() != null) {
            for (Map.Entry<String, String> entry : config.getArguments().entrySet()) {
                command.add("--" + entry.getKey());
                command.add(entry.getValue());
            }
        }
        
        // Set up process
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(reportsDir.toFile());
        
        // Redirect output to files
        Path stdoutFile = reportsDir.resolve("stdout.log");
        Path stderrFile = reportsDir.resolve("stderr.log");
        pb.redirectOutput(stdoutFile.toFile());
        pb.redirectError(stderrFile.toFile());
        
        log.info("Executing command: {}", String.join(" ", command));
        
        Process process = pb.start();
        
        long timeoutMs = config.getTimeoutMs() != null ? config.getTimeoutMs() : 300000; // 5 minutes default
        boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        
        if (!finished) {
            process.destroyForcibly();
            return TaskResult.failure("Process timed out after " + timeoutMs + "ms");
        }
        
        int exitCode = process.exitValue();
        
        // Read output files
        List<String> outputFiles = new ArrayList<>();
        if (Files.exists(stdoutFile)) outputFiles.add(stdoutFile.toString());
        if (Files.exists(stderrFile)) outputFiles.add(stderrFile.toString());
        
        // Add any additional output files created by the process
        Files.list(reportsDir)
            .filter(p -> !p.equals(stdoutFile) && !p.equals(stderrFile))
            .map(Path::toString)
            .forEach(outputFiles::add);
        
        TaskResult result;
        if (exitCode == 0) {
            result = TaskResult.success();
        } else {
            String errorMsg = Files.exists(stderrFile) ? 
                Files.readString(stderrFile).trim() : "Process failed with exit code " + exitCode;
            result = TaskResult.failure(errorMsg, exitCode);
        }
        
        result.setOutputFiles(outputFiles);
        return result;
    }
    
    private TaskResult executeSpi(ResolvedArtifact artifact, ExecutionConfig config, Path reportsDir) throws Exception {
        log.info("Executing SPI mode for {}", artifact.getPath());
        
        // Create isolated classloader
        URL[] urls = {new File(artifact.getPath()).toURI().toURL()};
        
        // Add dependencies to classpath if needed
        if (artifact.getDependencies() != null && !artifact.getDependencies().isEmpty()) {
            List<URL> allUrls = new ArrayList<>();
            allUrls.add(urls[0]);
            
            for (String dep : artifact.getDependencies()) {
                // In a real implementation, you'd resolve dependency paths
                // For now, we assume dependencies are available in the system classpath
                log.debug("Dependency: {}", dep);
            }
            
            urls = allUrls.toArray(new URL[0]);
        }
        
        try (URLClassLoader classLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader())) {
            // Discover Task implementations
            ServiceLoader<Task> taskLoader = ServiceLoader.load(Task.class, classLoader);
            
            Task task = null;
            for (Task t : taskLoader) {
                task = t;
                break; // Use first available task
            }
            
            if (task == null) {
                // Try to load com.acme.spi.Task directly
                try {
                    Class<?> taskClass = classLoader.loadClass("com.acme.spi.Task");
                    if (taskClass.isInterface()) {
                        // Look for implementations
                        return TaskResult.failure("No Task implementation found in artifact");
                    }
                } catch (ClassNotFoundException e) {
                    return TaskResult.failure("No Task interface or implementation found in artifact");
                }
            }
            
            log.info("Found task implementation: {}", task.getClass().getName());
            
            // Execute task
            Map<String, String> args = config.getArguments() != null ? config.getArguments() : new HashMap<>();
            TaskResult result = task.run(args, reportsDir);
            
            // Ensure output files are captured
            List<String> outputFiles = new ArrayList<>();
            Files.list(reportsDir)
                .map(Path::toString)
                .forEach(outputFiles::add);
            
            if (result.getOutputFiles() == null) {
                result.setOutputFiles(outputFiles);
            } else {
                result.getOutputFiles().addAll(outputFiles);
            }
            
            return result;
        }
    }
}