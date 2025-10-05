package com.acme.astra.app.cli.action;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Registry for managing and executing CLI actions.
 */
@Component
public class ActionRegistry {
    
    private final List<CliAction> actions;
    
    @Autowired
    public ActionRegistry(List<CliAction> actions) {
        this.actions = actions;
    }
    
    /**
     * Execute an action by name.
     * 
     * @param actionName Name of the action to execute
     * @param args Arguments to pass to the action
     * @return Exit code from the action
     */
    public int executeAction(String actionName, Map<String, String> args) {
        Optional<CliAction> action = findAction(actionName);
        
        if (action.isEmpty()) {
            System.err.println("❌ Unknown action: " + actionName);
            System.err.println("Available actions: " + getAvailableActionNames());
            printAllActionsUsage();
            return 1;
        }
        
        return action.get().execute(args);
    }
    
    /**
     * Find an action by name.
     */
    public Optional<CliAction> findAction(String actionName) {
        return actions.stream()
                .filter(action -> action.getName().equals(actionName))
                .findFirst();
    }
    
    /**
     * Get all available action names.
     */
    public List<String> getAvailableActionNames() {
        return actions.stream()
                .map(CliAction::getName)
                .sorted()
                .collect(Collectors.toList());
    }
    
    /**
     * Print usage information for a specific action.
     */
    public void printActionUsage(String actionName) {
        Optional<CliAction> action = findAction(actionName);
        if (action.isPresent()) {
            action.get().printUsage();
        } else {
            System.err.println("Unknown action: " + actionName);
            printAllActionsUsage();
        }
    }
    
    /**
     * Print usage information for all available actions.
     */
    public void printAllActionsUsage() {
        System.out.println();
        System.out.println("📋 Available Actions:");
        System.out.println();
        
        actions.stream()
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .forEach(action -> {
                    System.out.println("  " + action.getName());
                    System.out.println("    " + action.getDescription());
                    System.out.println();
                });
        
        System.out.println("For detailed usage of a specific action:");
        System.out.println("  astra-runner --action=<action-name> --help");
        System.out.println();
    }
    
    /**
     * Check if an action exists.
     */
    public boolean hasAction(String actionName) {
        return findAction(actionName).isPresent();
    }
    
    /**
     * Get the total number of registered actions.
     */
    public int getActionCount() {
        return actions.size();
    }
}