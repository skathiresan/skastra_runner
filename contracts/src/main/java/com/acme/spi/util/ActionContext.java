package com.acme.spi.util;

import java.util.Map;

/**
 * Utility interface that can be injected into Task implementations 
 * to provide access to Astra Runner actions and utilities.
 * 
 * This allows Task implementations to leverage built-in functionality
 * like system information gathering, argument processing, etc.
 */
public interface ActionContext {
    
    /**
     * Execute a CLI action programmatically.
     * 
     * @param actionName Name of the action to execute
     * @param args Arguments to pass to the action
     * @return Exit code (0 = success, non-zero = failure)
     */
    int executeAction(String actionName, Map<String, String> args);
    
    /**
     * Get system information in a structured format.
     * Equivalent to calling system-info action with JSON format.
     * 
     * @return Map containing system information
     */
    Map<String, Object> getSystemInfo();
    
    /**
     * Get all runtime arguments that were passed to the Astra Runner.
     * Useful for Tasks that need access to global configuration.
     * 
     * @return Map of all runtime arguments
     */
    Map<String, String> getRuntimeArguments();
    
    /**
     * Check if a specific action is available.
     * 
     * @param actionName Name of the action to check
     * @return true if the action exists and can be executed
     */
    boolean isActionAvailable(String actionName);
    
    /**
     * Get list of all available action names.
     * 
     * @return List of action names
     */
    java.util.List<String> getAvailableActions();
    
    /**
     * Get the workspace directory being used by the current execution.
     * 
     * @return Path to the workspace directory
     */
    java.nio.file.Path getWorkspaceDirectory();
    
    /**
     * Get the Astra Runner version information.
     * 
     * @return Version string
     */
    String getAstraRunnerVersion();
}