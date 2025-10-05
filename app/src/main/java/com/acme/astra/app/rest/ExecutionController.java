package com.acme.astra.app.rest;

import com.acme.astra.engine.AstraEngine;
import com.acme.astra.model.ExecutionConfig;
import com.acme.astra.model.RunSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

/**
 * REST API controller for remote execution.
 */
@RestController
@RequestMapping("/api/v1")
public class ExecutionController {
    
    private static final Logger log = LoggerFactory.getLogger(ExecutionController.class);
    
    private final AstraEngine engine;
    
    @Autowired
    public ExecutionController(AstraEngine engine) {
        this.engine = engine;
    }
    
    /**
     * Execute artifact synchronously.
     */
    @PostMapping("/execute")
    public ResponseEntity<RunSummary> execute(@RequestBody ExecutionRequest request) {
        log.info("Received execution request for {}:{}", request.getGroupArtifact(), request.getVersion());
        
        try {
            ExecutionConfig config = convertToConfig(request);
            
            // Create workspace directory
            Path workspace = Paths.get(request.getWorkspaceDir() != null ? request.getWorkspaceDir() : "./workspace");
            Files.createDirectories(workspace);
            
            RunSummary summary = engine.execute(config, workspace);
            
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            log.error("Execution failed", e);
            
            // Return error summary
            RunSummary errorSummary = new RunSummary("error");
            RunSummary.ExecutionResult result = new RunSummary.ExecutionResult();
            result.setStatus("FAILURE");
            result.setMessage("Execution failed: " + e.getMessage());
            errorSummary.setExecutionResult(result);
            
            return ResponseEntity.status(500).body(errorSummary);
        }
    }
    
    /**
     * Execute artifact asynchronously.
     */
    @PostMapping("/execute/async")
    public ResponseEntity<AsyncExecutionResponse> executeAsync(@RequestBody ExecutionRequest request) {
        log.info("Received async execution request for {}:{}", request.getGroupArtifact(), request.getVersion());
        
        String executionId = java.util.UUID.randomUUID().toString();
        
        CompletableFuture.supplyAsync(() -> {
            try {
                ExecutionConfig config = convertToConfig(request);
                
                // Create workspace directory
                Path workspace = Paths.get(request.getWorkspaceDir() != null ? request.getWorkspaceDir() : "./workspace");
                Files.createDirectories(workspace);
                
                return engine.execute(config, workspace);
                
            } catch (Exception e) {
                log.error("Async execution failed", e);
                throw new RuntimeException(e);
            }
        }).whenComplete((summary, throwable) -> {
            if (throwable != null) {
                log.error("Async execution {} failed", executionId, throwable);
            } else {
                log.info("Async execution {} completed: {}", executionId, summary.getSummary());
            }
        });
        
        AsyncExecutionResponse response = new AsyncExecutionResponse();
        response.setExecutionId(executionId);
        response.setStatus("STARTED");
        response.setMessage("Execution started asynchronously");
        
        return ResponseEntity.accepted().body(response);
    }
    
    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = new HealthResponse();
        response.setStatus("UP");
        response.setMessage("Astra Runner is running");
        response.setTimestamp(java.time.Instant.now());
        
        // Check Artifactory connectivity
        String artifactoryUser = System.getenv("ARTIFACTORY_USER");
        String artifactoryPassword = System.getenv("ARTIFACTORY_PASSWORD");
        
        if (artifactoryUser == null || artifactoryPassword == null) {
            response.setArtifactoryStatus("NOT_CONFIGURED");
        } else {
            response.setArtifactoryStatus("CONFIGURED");
        }
        
        return ResponseEntity.ok(response);
    }
    
    private ExecutionConfig convertToConfig(ExecutionRequest request) {
        ExecutionConfig config = new ExecutionConfig(
            request.getGroupArtifact(),
            request.getVersion(),
            request.getMode()
        );
        
        config.setArguments(request.getArguments());
        config.setReportsDir(request.getReportsDir() != null ? request.getReportsDir() : "./reports");
        config.setTimeoutMs(request.getTimeoutMs() != null ? request.getTimeoutMs() : 300000L);
        config.setJvmArgs(request.getJvmArgs());
        
        return config;
    }
    
    // DTOs
    public static class ExecutionRequest {
        private String groupArtifact;
        private String version;
        private ExecutionConfig.ExecutionMode mode;
        private java.util.Map<String, String> arguments;
        private String reportsDir;
        private String workspaceDir;
        private Long timeoutMs;
        private java.util.List<String> jvmArgs;
        
        // Getters and setters
        public String getGroupArtifact() { return groupArtifact; }
        public void setGroupArtifact(String groupArtifact) { this.groupArtifact = groupArtifact; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public ExecutionConfig.ExecutionMode getMode() { return mode; }
        public void setMode(ExecutionConfig.ExecutionMode mode) { this.mode = mode; }
        
        public java.util.Map<String, String> getArguments() { return arguments; }
        public void setArguments(java.util.Map<String, String> arguments) { this.arguments = arguments; }
        
        public String getReportsDir() { return reportsDir; }
        public void setReportsDir(String reportsDir) { this.reportsDir = reportsDir; }
        
        public String getWorkspaceDir() { return workspaceDir; }
        public void setWorkspaceDir(String workspaceDir) { this.workspaceDir = workspaceDir; }
        
        public Long getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(Long timeoutMs) { this.timeoutMs = timeoutMs; }
        
        public java.util.List<String> getJvmArgs() { return jvmArgs; }
        public void setJvmArgs(java.util.List<String> jvmArgs) { this.jvmArgs = jvmArgs; }
    }
    
    public static class AsyncExecutionResponse {
        private String executionId;
        private String status;
        private String message;
        
        // Getters and setters
        public String getExecutionId() { return executionId; }
        public void setExecutionId(String executionId) { this.executionId = executionId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    public static class HealthResponse {
        private String status;
        private String message;
        private java.time.Instant timestamp;
        private String artifactoryStatus;
        
        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public java.time.Instant getTimestamp() { return timestamp; }
        public void setTimestamp(java.time.Instant timestamp) { this.timestamp = timestamp; }
        
        public String getArtifactoryStatus() { return artifactoryStatus; }
        public void setArtifactoryStatus(String artifactoryStatus) { this.artifactoryStatus = artifactoryStatus; }
    }
}