package com.acme.astra.engine.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for executing command line processes with comprehensive monitoring and control.
 * Provides a fluent API for building and executing commands with timeout, output capture,
 * environment variables, and detailed execution results.
 */
public class CommandExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(CommandExecutor.class);
    
    private final List<String> command;
    private Path workingDirectory;
    private Map<String, String> environment;
    private long timeoutSeconds = 300; // 5 minutes default
    private boolean captureOutput = true;
    private boolean inheritIO = false;
    private Path stdoutFile;
    private Path stderrFile;
    private Path outputDirectory;
    
    private CommandExecutor(List<String> command) {
        this.command = new ArrayList<>(command);
        this.environment = new HashMap<>();
    }
    
    /**
     * Create a new CommandExecutor for the given command.
     */
    public static CommandExecutor of(String... commandParts) {
        return new CommandExecutor(List.of(commandParts));
    }
    
    /**
     * Create a new CommandExecutor for the given command list.
     */
    public static CommandExecutor of(List<String> command) {
        return new CommandExecutor(command);
    }
    
    /**
     * Parse a command string into parts (simple space-based splitting).
     * For more complex parsing, use of(String...) or of(List<String>).
     */
    public static CommandExecutor parse(String commandString) {
        if (commandString == null || commandString.trim().isEmpty()) {
            throw new IllegalArgumentException("Command string cannot be null or empty");
        }
        
        // Simple space-based splitting - you might want to enhance this for quoted arguments
        String[] parts = commandString.trim().split("\\s+");
        return new CommandExecutor(List.of(parts));
    }
    
    /**
     * Set the working directory for command execution.
     */
    public CommandExecutor workingDirectory(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }
    
    /**
     * Set the working directory for command execution.
     */
    public CommandExecutor workingDirectory(String workingDirectory) {
        return workingDirectory(Path.of(workingDirectory));
    }
    
    /**
     * Set environment variables for the command.
     */
    public CommandExecutor environment(Map<String, String> environment) {
        this.environment.clear();
        this.environment.putAll(environment);
        return this;
    }
    
    /**
     * Add a single environment variable.
     */
    public CommandExecutor env(String key, String value) {
        this.environment.put(key, value);
        return this;
    }
    
    /**
     * Set the timeout for command execution.
     */
    public CommandExecutor timeout(long seconds) {
        this.timeoutSeconds = seconds;
        return this;
    }
    
    /**
     * Set whether to capture stdout/stderr to files.
     */
    public CommandExecutor captureOutput(boolean capture) {
        this.captureOutput = capture;
        return this;
    }
    
    /**
     * Set whether to inherit IO from parent process.
     */
    public CommandExecutor inheritIO(boolean inherit) {
        this.inheritIO = inherit;
        return this;
    }
    
    /**
     * Set custom stdout file location.
     */
    public CommandExecutor stdoutFile(Path stdoutFile) {
        this.stdoutFile = stdoutFile;
        return this;
    }
    
    /**
     * Set custom stderr file location.
     */
    public CommandExecutor stderrFile(Path stderrFile) {
        this.stderrFile = stderrFile;
        return this;
    }
    
    /**
     * Set output directory (stdout and stderr files will be created here).
     */
    public CommandExecutor outputDirectory(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }
    
    /**
     * Execute the command and return the result.
     */
    public CommandResult execute() throws IOException, InterruptedException {
        Instant startTime = Instant.now();
        
        log.info("Executing command: {}", String.join(" ", command));
        log.debug("Working directory: {}", workingDirectory);
        log.debug("Timeout: {} seconds", timeoutSeconds);
        log.debug("Environment variables: {}", environment.size());
        
        // Validate command
        if (command.isEmpty()) {
            throw new IllegalStateException("Command cannot be empty");
        }
        
        // Set up ProcessBuilder
        ProcessBuilder pb = new ProcessBuilder(command);
        
        if (workingDirectory != null) {
            pb.directory(workingDirectory.toFile());
        }
        
        // Set environment variables
        pb.environment().putAll(environment);
        
        // Set up output handling
        setupOutputHandling(pb);
        
        try {
            // Start the process
            Process process = pb.start();
            
            // Wait for completion with timeout
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            Instant endTime = Instant.now();
            long durationMs = endTime.toEpochMilli() - startTime.toEpochMilli();
            
            if (!finished) {
                log.warn("Command timed out after {} seconds, destroying process", timeoutSeconds);
                process.destroyForcibly();
                
                return CommandResult.builder()
                    .command(command)
                    .workingDirectory(workingDirectory)
                    .startTime(startTime)
                    .endTime(endTime)
                    .durationMs(durationMs)
                    .timedOut(true)
                    .exitCode(-1)
                    .success(false)
                    .stdoutFile(stdoutFile)
                    .stderrFile(stderrFile)
                    .errorMessage("Command timed out after " + timeoutSeconds + " seconds")
                    .build();
            }
            
            int exitCode = process.exitValue();
            boolean success = exitCode == 0;
            
            log.info("Command completed with exit code {} in {}ms", exitCode, durationMs);
            
            // Read error output if available and command failed
            String errorMessage = null;
            if (!success && stderrFile != null && Files.exists(stderrFile)) {
                try {
                    String stderr = Files.readString(stderrFile);
                    if (!stderr.trim().isEmpty()) {
                        errorMessage = stderr.length() > 1000 ? 
                            stderr.substring(0, 1000) + "... (truncated)" : stderr;
                    }
                } catch (IOException e) {
                    log.warn("Failed to read stderr file: {}", e.getMessage());
                }
            }
            
            return CommandResult.builder()
                .command(command)
                .workingDirectory(workingDirectory)
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(durationMs)
                .timedOut(false)
                .exitCode(exitCode)
                .success(success)
                .stdoutFile(stdoutFile)
                .stderrFile(stderrFile)
                .errorMessage(errorMessage)
                .build();
                
        } catch (IOException e) {
            Instant endTime = Instant.now();
            long durationMs = endTime.toEpochMilli() - startTime.toEpochMilli();
            
            log.error("Failed to execute command: {}", e.getMessage());
            
            return CommandResult.builder()
                .command(command)
                .workingDirectory(workingDirectory)
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(durationMs)
                .timedOut(false)
                .exitCode(-1)
                .success(false)
                .errorMessage("Failed to execute command: " + e.getMessage())
                .build();
        }
    }
    
    private void setupOutputHandling(ProcessBuilder pb) throws IOException {
        if (inheritIO) {
            pb.inheritIO();
            return;
        }
        
        if (captureOutput) {
            // Create output directory if specified
            if (outputDirectory != null) {
                Files.createDirectories(outputDirectory);
                if (stdoutFile == null) {
                    stdoutFile = outputDirectory.resolve("stdout.log");
                }
                if (stderrFile == null) {
                    stderrFile = outputDirectory.resolve("stderr.log");
                }
            }
            
            // Set default file names if not specified
            if (stdoutFile == null) {
                stdoutFile = Path.of("stdout.log");
            }
            if (stderrFile == null) {
                stderrFile = Path.of("stderr.log");
            }
            
            // Ensure parent directories exist
            Files.createDirectories(stdoutFile.getParent());
            Files.createDirectories(stderrFile.getParent());
            
            pb.redirectOutput(stdoutFile.toFile());
            pb.redirectError(stderrFile.toFile());
            
            log.debug("Output will be captured to: stdout={}, stderr={}", stdoutFile, stderrFile);
        }
    }
    
    /**
     * Result of command execution containing all relevant information.
     */
    public static class CommandResult {
        private final List<String> command;
        private final Path workingDirectory;
        private final Instant startTime;
        private final Instant endTime;
        private final long durationMs;
        private final boolean timedOut;
        private final int exitCode;
        private final boolean success;
        private final Path stdoutFile;
        private final Path stderrFile;
        private final String errorMessage;
        
        private CommandResult(Builder builder) {
            this.command = builder.command;
            this.workingDirectory = builder.workingDirectory;
            this.startTime = builder.startTime;
            this.endTime = builder.endTime;
            this.durationMs = builder.durationMs;
            this.timedOut = builder.timedOut;
            this.exitCode = builder.exitCode;
            this.success = builder.success;
            this.stdoutFile = builder.stdoutFile;
            this.stderrFile = builder.stderrFile;
            this.errorMessage = builder.errorMessage;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public List<String> getCommand() { return command; }
        public Path getWorkingDirectory() { return workingDirectory; }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public long getDurationMs() { return durationMs; }
        public boolean isTimedOut() { return timedOut; }
        public int getExitCode() { return exitCode; }
        public boolean isSuccess() { return success; }
        public Path getStdoutFile() { return stdoutFile; }
        public Path getStderrFile() { return stderrFile; }
        public String getErrorMessage() { return errorMessage; }
        
        /**
         * Get stdout content if available.
         */
        public String getStdout() throws IOException {
            if (stdoutFile != null && Files.exists(stdoutFile)) {
                return Files.readString(stdoutFile);
            }
            return null;
        }
        
        /**
         * Get stderr content if available.
         */
        public String getStderr() throws IOException {
            if (stderrFile != null && Files.exists(stderrFile)) {
                return Files.readString(stderrFile);
            }
            return null;
        }
        
        /**
         * Get a summary of the execution.
         */
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Command: ").append(String.join(" ", command)).append("\n");
            sb.append("Exit Code: ").append(exitCode).append("\n");
            sb.append("Duration: ").append(durationMs).append("ms\n");
            sb.append("Success: ").append(success).append("\n");
            if (timedOut) {
                sb.append("Status: TIMED OUT\n");
            }
            if (errorMessage != null) {
                sb.append("Error: ").append(errorMessage).append("\n");
            }
            return sb.toString();
        }
        
        public static class Builder {
            private List<String> command;
            private Path workingDirectory;
            private Instant startTime;
            private Instant endTime;
            private long durationMs;
            private boolean timedOut;
            private int exitCode;
            private boolean success;
            private Path stdoutFile;
            private Path stderrFile;
            private String errorMessage;
            
            public Builder command(List<String> command) { this.command = command; return this; }
            public Builder workingDirectory(Path workingDirectory) { this.workingDirectory = workingDirectory; return this; }
            public Builder startTime(Instant startTime) { this.startTime = startTime; return this; }
            public Builder endTime(Instant endTime) { this.endTime = endTime; return this; }
            public Builder durationMs(long durationMs) { this.durationMs = durationMs; return this; }
            public Builder timedOut(boolean timedOut) { this.timedOut = timedOut; return this; }
            public Builder exitCode(int exitCode) { this.exitCode = exitCode; return this; }
            public Builder success(boolean success) { this.success = success; return this; }
            public Builder stdoutFile(Path stdoutFile) { this.stdoutFile = stdoutFile; return this; }
            public Builder stderrFile(Path stderrFile) { this.stderrFile = stderrFile; return this; }
            public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
            
            public CommandResult build() {
                return new CommandResult(this);
            }
        }
    }
}