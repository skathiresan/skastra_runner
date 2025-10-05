package com.acme.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Result of task execution containing status, metrics, and output references.
 */
public class TaskResult {
    
    public enum Status {
        SUCCESS, FAILURE, SKIPPED, TIMEOUT
    }
    
    @JsonProperty("status")
    private Status status;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("exitCode")
    private Integer exitCode;
    
    @JsonProperty("startTime")
    private Instant startTime;
    
    @JsonProperty("endTime") 
    private Instant endTime;
    
    @JsonProperty("duration")
    private Long durationMs;
    
    @JsonProperty("outputFiles")
    private List<String> outputFiles;
    
    @JsonProperty("metrics")
    private Map<String, Object> metrics;
    
    @JsonProperty("errors")
    private List<String> errors;
    
    // Constructors
    public TaskResult() {}
    
    public TaskResult(Status status, String message) {
        this.status = status;
        this.message = message;
        this.startTime = Instant.now();
        this.endTime = Instant.now();
        this.durationMs = 0L;
    }
    
    // Builder pattern
    public static TaskResult success() {
        return new TaskResult(Status.SUCCESS, "Task completed successfully");
    }
    
    public static TaskResult failure(String message) {
        return new TaskResult(Status.FAILURE, message);
    }
    
    public static TaskResult failure(String message, int exitCode) {
        TaskResult result = new TaskResult(Status.FAILURE, message);
        result.exitCode = exitCode;
        return result;
    }
    
    // Getters and setters
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Integer getExitCode() { return exitCode; }
    public void setExitCode(Integer exitCode) { this.exitCode = exitCode; }
    
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    
    public List<String> getOutputFiles() { return outputFiles; }
    public void setOutputFiles(List<String> outputFiles) { this.outputFiles = outputFiles; }
    
    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }
    
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    
    // Fluent setters
    public TaskResult withExitCode(int exitCode) {
        this.exitCode = exitCode;
        return this;
    }
    
    public TaskResult withOutputFiles(List<String> outputFiles) {
        this.outputFiles = outputFiles;
        return this;
    }
    
    public TaskResult withMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
        return this;
    }
    
    public TaskResult withErrors(List<String> errors) {
        this.errors = errors;
        return this;
    }
    
    public TaskResult withTiming(Instant startTime, Instant endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMs = endTime.toEpochMilli() - startTime.toEpochMilli();
        return this;
    }
}