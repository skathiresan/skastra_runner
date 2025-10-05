package com.acme.astra.engine.util;

import com.acme.spi.TaskResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for converting CommandExecutor results to TaskResults.
 * Provides convenient methods for mapping command execution outcomes to the SPI TaskResult format.
 */
public class TaskResultMapper {
    
    /**
     * Convert a CommandResult to a TaskResult with basic mapping.
     */
    public static TaskResult toTaskResult(CommandExecutor.CommandResult commandResult) {
        return toTaskResult(commandResult, null, null);
    }
    
    /**
     * Convert a CommandResult to a TaskResult with additional output files and metrics.
     */
    public static TaskResult toTaskResult(CommandExecutor.CommandResult commandResult, 
                                        List<String> additionalOutputFiles,
                                        Map<String, Object> additionalMetrics) {
        
        // Determine status and message
        TaskResult.Status status;
        String message;
        
        if (commandResult.isTimedOut()) {
            status = TaskResult.Status.TIMEOUT;
            message = "Command timed out after execution";
        } else if (commandResult.isSuccess()) {
            status = TaskResult.Status.SUCCESS;
            message = "Command executed successfully";
        } else {
            status = TaskResult.Status.FAILURE;
            message = commandResult.getErrorMessage() != null ? 
                commandResult.getErrorMessage() : 
                "Command failed with exit code " + commandResult.getExitCode();
        }
        
        // Create TaskResult
        TaskResult result = new TaskResult(status, message);
        result.setExitCode(commandResult.getExitCode());
        result.setStartTime(commandResult.getStartTime());
        result.setEndTime(commandResult.getEndTime());
        result.setDurationMs(commandResult.getDurationMs());
        
        // Collect output files
        List<String> outputFiles = new ArrayList<>();
        if (commandResult.getStdoutFile() != null) {
            outputFiles.add(commandResult.getStdoutFile().toString());
        }
        if (commandResult.getStderrFile() != null) {
            outputFiles.add(commandResult.getStderrFile().toString());
        }
        if (additionalOutputFiles != null) {
            outputFiles.addAll(additionalOutputFiles);
        }
        result.setOutputFiles(outputFiles);
        
        // Create metrics
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("command", String.join(" ", commandResult.getCommand()));
        metrics.put("exitCode", commandResult.getExitCode());
        metrics.put("durationMs", commandResult.getDurationMs());
        metrics.put("timedOut", commandResult.isTimedOut());
        metrics.put("success", commandResult.isSuccess());
        
        if (commandResult.getWorkingDirectory() != null) {
            metrics.put("workingDirectory", commandResult.getWorkingDirectory().toString());
        }
        
        if (additionalMetrics != null) {
            metrics.putAll(additionalMetrics);
        }
        result.setMetrics(metrics);
        
        // Set error information if available
        if (!commandResult.isSuccess()) {
            List<String> errors = new ArrayList<>();
            if (commandResult.getErrorMessage() != null) {
                errors.add(commandResult.getErrorMessage());
            }
            
            // Try to add stderr content as error if available
            try {
                String stderr = commandResult.getStderr();
                if (stderr != null && !stderr.trim().isEmpty()) {
                    String truncatedStderr = stderr.length() > 500 ? 
                        stderr.substring(0, 500) + "... (truncated)" : stderr;
                    errors.add("STDERR: " + truncatedStderr);
                }
            } catch (IOException e) {
                errors.add("Failed to read stderr: " + e.getMessage());
            }
            
            if (!errors.isEmpty()) {
                result.setErrors(errors);
            }
        }
        
        return result;
    }
    
    /**
     * Create a simple success TaskResult for command execution.
     */
    public static TaskResult success(CommandExecutor.CommandResult commandResult) {
        return toTaskResult(commandResult)
            .withMetrics(Map.of(
                "executionType", "command",
                "commandString", String.join(" ", commandResult.getCommand())
            ));
    }
    
    /**
     * Create a failure TaskResult for command execution.
     */
    public static TaskResult failure(CommandExecutor.CommandResult commandResult, String customMessage) {
        TaskResult result = toTaskResult(commandResult);
        if (customMessage != null) {
            result.setMessage(customMessage);
        }
        return result;
    }
    
    /**
     * Create a TaskResult with custom metrics and output files.
     */
    public static TaskResult withCustomData(CommandExecutor.CommandResult commandResult,
                                          Map<String, Object> customMetrics,
                                          List<String> customOutputFiles) {
        return toTaskResult(commandResult, customOutputFiles, customMetrics);
    }
}