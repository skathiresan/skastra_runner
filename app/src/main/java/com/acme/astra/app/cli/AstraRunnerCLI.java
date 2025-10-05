package com.acme.astra.app.cli;

import com.acme.astra.engine.AstraEngine;
import com.acme.astra.model.ExecutionConfig;
import com.acme.astra.model.RunSummary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.spring.PicocliSpringFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Command-line interface for the Astra Runner.
 */
@Component
@Command(name = "astra-runner", mixinStandardHelpOptions = true, version = "1.0.0",
         description = "Executes versioned Java libraries from Artifactory")
public class AstraRunnerCLI implements CommandLineRunner, Callable<Integer>, ExitCodeGenerator {
    
    private static final Logger log = LoggerFactory.getLogger(AstraRunnerCLI.class);
    
    @Option(names = {"--ga", "--group-artifact"}, 
            description = "Group:Artifact identifier (e.g., com.example:my-lib)", 
            required = true)
    private String groupArtifact;
    
    @Option(names = {"--version", "-v"}, 
            description = "Version (exact, latest.release, or semver pattern)", 
            required = true)
    private String version;
    
    @Option(names = {"--mode", "-m"}, 
            description = "Execution mode: ${COMPLETION-CANDIDATES}", 
            required = true)
    private ExecutionConfig.ExecutionMode mode;
    
    @Option(names = {"--argsJson", "--args"}, 
            description = "Runtime arguments as JSON string", 
            defaultValue = "{}")
    private String argsJson;
    
    @Option(names = {"--reportsDir", "--reports"}, 
            description = "Output directory for reports", 
            defaultValue = "./reports")
    private String reportsDir;
    
    @Option(names = {"--workspace", "-w"}, 
            description = "Workspace directory for temporary files", 
            defaultValue = "./workspace")
    private String workspaceDir;
    
    @Option(names = {"--timeout"}, 
            description = "Execution timeout in milliseconds", 
            defaultValue = "300000")
    private long timeoutMs;
    
    @Option(names = {"--jvmArgs"}, 
            description = "Additional JVM arguments (comma-separated)")
    private String jvmArgs;
    
    @Option(names = {"--rest"}, 
            description = "Start REST API server instead of CLI execution")
    private boolean restMode;
    
    private final AstraEngine engine;
    private final ObjectMapper objectMapper;
    private int exitCode = 0;
    
    @Autowired
    public AstraRunnerCLI(AstraEngine engine) {
        this.engine = engine;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public void run(String... args) throws Exception {
        // Use PicoCLI to parse command line arguments
        CommandLine cmd = new CommandLine(this);
        exitCode = cmd.execute(args);
    }
    
    @Override
    public Integer call() throws Exception {
        if (restMode) {
            log.info("Starting in REST API mode - server will continue running");
            return 0; // Let Spring Boot continue running the web server
        }
        
        // Check if required parameters are provided
        if (groupArtifact == null || version == null || mode == null) {
            log.info("No CLI execution parameters provided - starting in REST API mode");
            log.info("Use --help to see available options");
            return 0;
        }
        
        log.info("Starting Astra Runner CLI");
        log.info("Group:Artifact: {}", groupArtifact);
        log.info("Version: {}", version);
        log.info("Mode: {}", mode);
        log.info("Reports Directory: {}", reportsDir);
        
        try {
            // Parse arguments JSON
            Map<String, String> arguments = objectMapper.readValue(argsJson, new TypeReference<Map<String, String>>() {});
            
            // Create execution configuration
            ExecutionConfig config = new ExecutionConfig(groupArtifact, version, mode);
            config.setArguments(arguments);
            config.setReportsDir(reportsDir);
            config.setTimeoutMs(timeoutMs);
            
            if (jvmArgs != null && !jvmArgs.trim().isEmpty()) {
                config.setJvmArgs(java.util.Arrays.asList(jvmArgs.split(",")));
            }
            
            // Create workspace directory
            Path workspace = Paths.get(workspaceDir);
            Files.createDirectories(workspace);
            
            // Execute
            RunSummary summary = engine.execute(config, workspace);
            
            // Print summary
            log.info("Execution completed: {}", summary.getSummary());
            log.info("Reports available in: {}", reportsDir);
            
            // Return appropriate exit code
            if (summary.getExecutionResult() != null && summary.getExecutionResult().getExitCode() != null) {
                return summary.getExecutionResult().getExitCode();
            } else if ("FAILURE".equals(summary.getExecutionResult().getStatus())) {
                return 1;
            } else {
                return 0;
            }
            
        } catch (Exception e) {
            log.error("Execution failed", e);
            return 1;
        }
    }
    
    @Override
    public int getExitCode() {
        return exitCode;
    }
}