package com.acme.astra.app.cli.action;

import java.util.Map;

/**
 * Interface for CLI actions that can be executed by the Astra Runner.
 * Actions provide a way to perform utility operations without executing artifacts.
 */
public interface CliAction {
    
    /**
     * Get the name of this action (used for CLI invocation).
     */
    String getName();
    
    /**
     * Get a description of what this action does.
     */
    String getDescription();
    
    /**
     * Execute the action with the provided arguments.
     * 
     * @param args Arguments passed to the action
     * @return Exit code (0 for success, non-zero for failure)
     */
    int execute(Map<String, String> args);
    
    /**
     * Print usage information for this action.
     */
    void printUsage();
}