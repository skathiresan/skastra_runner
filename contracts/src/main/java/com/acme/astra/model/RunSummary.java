package com.acme.astra.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Summary of execution results including artifact details, timing, and outcomes.
 */
public class RunSummary {
    
    @JsonProperty("executionId")
    private String executionId;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("config")
    private ExecutionConfig config;
    
    @JsonProperty("resolvedArtifact")
    private ResolvedArtifact resolvedArtifact;
    
    @JsonProperty("executionResult")
    private ExecutionResult executionResult;
    
    @JsonProperty("outputFiles")
    private List<String> outputFiles;
    
    @JsonProperty("summary")
    private String summary;
    
    public static class ResolvedArtifact {
        @JsonProperty("groupId")
        private String groupId;
        
        @JsonProperty("artifactId")
        private String artifactId;
        
        @JsonProperty("version")
        private String version;
        
        @JsonProperty("sha256")
        private String sha256;
        
        @JsonProperty("path")
        private String path;
        
        @JsonProperty("dependencies")
        private List<String> dependencies;
        
        // Getters and setters
        public String getGroupId() { return groupId; }
        public void setGroupId(String groupId) { this.groupId = groupId; }
        
        public String getArtifactId() { return artifactId; }
        public void setArtifactId(String artifactId) { this.artifactId = artifactId; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public String getSha256() { return sha256; }
        public void setSha256(String sha256) { this.sha256 = sha256; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public List<String> getDependencies() { return dependencies; }
        public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }
    }
    
    public static class ExecutionResult {
        @JsonProperty("status")
        private String status;
        
        @JsonProperty("exitCode")
        private Integer exitCode;
        
        @JsonProperty("startTime")
        private Instant startTime;
        
        @JsonProperty("endTime")
        private Instant endTime;
        
        @JsonProperty("durationMs")
        private Long durationMs;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("errors")
        private List<String> errors;
        
        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Integer getExitCode() { return exitCode; }
        public void setExitCode(Integer exitCode) { this.exitCode = exitCode; }
        
        public Instant getStartTime() { return startTime; }
        public void setStartTime(Instant startTime) { this.startTime = startTime; }
        
        public Instant getEndTime() { return endTime; }
        public void setEndTime(Instant endTime) { this.endTime = endTime; }
        
        public Long getDurationMs() { return durationMs; }
        public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
    }
    
    // Constructors
    public RunSummary() {}
    
    public RunSummary(String executionId) {
        this.executionId = executionId;
        this.timestamp = Instant.now();
    }
    
    // Getters and setters
    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public ExecutionConfig getConfig() { return config; }
    public void setConfig(ExecutionConfig config) { this.config = config; }
    
    public ResolvedArtifact getResolvedArtifact() { return resolvedArtifact; }
    public void setResolvedArtifact(ResolvedArtifact resolvedArtifact) { this.resolvedArtifact = resolvedArtifact; }
    
    public ExecutionResult getExecutionResult() { return executionResult; }
    public void setExecutionResult(ExecutionResult executionResult) { this.executionResult = executionResult; }
    
    public List<String> getOutputFiles() { return outputFiles; }
    public void setOutputFiles(List<String> outputFiles) { this.outputFiles = outputFiles; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}