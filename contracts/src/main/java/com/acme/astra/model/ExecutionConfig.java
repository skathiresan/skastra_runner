package com.acme.astra.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Configuration for task execution including artifact details and runtime parameters.
 */
public class ExecutionConfig {
    
    @JsonProperty("groupArtifact")
    private String groupArtifact;
    
    @JsonProperty("version")
    private String version;
    
    @JsonProperty("mode")
    private ExecutionMode mode;
    
    @JsonProperty("arguments")
    private Map<String, String> arguments;
    
    @JsonProperty("reportsDir")
    private String reportsDir;
    
    @JsonProperty("timeout")
    private Long timeoutMs;
    
    @JsonProperty("jvmArgs")
    private List<String> jvmArgs;
    
    public enum ExecutionMode {
        CLI, SPI
    }
    
    // Constructors
    public ExecutionConfig() {}
    
    public ExecutionConfig(String groupArtifact, String version, ExecutionMode mode) {
        this.groupArtifact = groupArtifact;
        this.version = version;
        this.mode = mode;
    }
    
    // Getters and setters
    public String getGroupArtifact() { return groupArtifact; }
    public void setGroupArtifact(String groupArtifact) { this.groupArtifact = groupArtifact; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public ExecutionMode getMode() { return mode; }
    public void setMode(ExecutionMode mode) { this.mode = mode; }
    
    public Map<String, String> getArguments() { return arguments; }
    public void setArguments(Map<String, String> arguments) { this.arguments = arguments; }
    
    public String getReportsDir() { return reportsDir; }
    public void setReportsDir(String reportsDir) { this.reportsDir = reportsDir; }
    
    public Long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Long timeoutMs) { this.timeoutMs = timeoutMs; }
    
    public List<String> getJvmArgs() { return jvmArgs; }
    public void setJvmArgs(List<String> jvmArgs) { this.jvmArgs = jvmArgs; }
}