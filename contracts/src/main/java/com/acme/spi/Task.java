package com.acme.spi;

import com.acme.spi.util.ActionContext;
import java.nio.file.Path;
import java.util.Map;

/**
 * Service Provider Interface for executable tasks.
 * Implementations should be discoverable via ServiceLoader.
 */
public interface Task {
    /**
     * Execute the task with given arguments and output directory.
     * 
     * @param args Runtime arguments as key-value pairs  
     * @param outDir Directory for output files
     * @return Task execution result
     * @throws Exception if execution fails
     */
    TaskResult run(Map<String, String> args, Path outDir) throws Exception;
    
    /**
     * Execute the task with access to Astra Runner's action system.
     * Tasks can override this method to access built-in functionality.
     * Default implementation calls the standard run method.
     * 
     * @param args Runtime arguments as key-value pairs
     * @param outDir Directory for output files
     * @param context Access to Astra Runner actions and utilities
     * @return Task execution result
     * @throws Exception if execution fails
     */
    default TaskResult run(Map<String, String> args, Path outDir, ActionContext context) throws Exception {
        return run(args, outDir);
    }
    
    /**
     * Get a human-readable name for this task.
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * Get the version of this task implementation.
     */
    default String getVersion() {
        return "unknown";
    }
}