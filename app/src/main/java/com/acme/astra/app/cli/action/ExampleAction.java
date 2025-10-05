package com.acme.astra.app.cli.action;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Example action demonstrating how to create new utility actions.
 * This action is commented out but serves as a template for future actions.
 */
// @Component  // Uncomment to enable this action
public class ExampleAction implements CliAction {
    
    @Override
    public String getName() {
        return "example";
    }
    
    @Override
    public String getDescription() {
        return "Example action demonstrating the action system (disabled by default)";
    }
    
    @Override
    public int execute(Map<String, String> args) {
        System.out.println("=== Example Action ===");
        System.out.println();
        
        // Example of accessing arguments
        String customParam = args.get("customParam");
        if (customParam != null) {
            System.out.println("Custom parameter provided: " + customParam);
        }
        
        // Example of different output formats
        String format = args.getOrDefault("format", "human");
        
        switch (format) {
            case "json" -> {
                System.out.println("{");
                System.out.println("  \"action\": \"example\",");
                System.out.println("  \"status\": \"success\",");
                System.out.println("  \"message\": \"Example action executed successfully\"");
                System.out.println("}");
            }
            default -> {
                System.out.println("🚀 Example Action Executed Successfully!");
                System.out.println();
                System.out.println("This is an example of how to create new actions.");
                System.out.println("You can:");
                System.out.println("  - Access arguments passed via CLI");
                System.out.println("  - Support different output formats");
                System.out.println("  - Perform any utility operations");
                System.out.println();
            }
        }
        
        // Example of argument summary
        System.out.println("Arguments received:");
        args.forEach((key, value) -> 
            System.out.println("  " + key + " = " + value));
        
        return 0; // Success
    }
    
    @Override
    public void printUsage() {
        System.out.println("Usage: astra-runner --action=" + getName() + " [options]");
        System.out.println();
        System.out.println("Description:");
        System.out.println("  " + getDescription());
        System.out.println();
        System.out.println("Options (via --args JSON):");
        System.out.println("  format=<format>        Output format: human, json (default: human)");
        System.out.println("  customParam=<value>    Example custom parameter");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  astra-runner --action=" + getName());
        System.out.println("  astra-runner --action=" + getName() + " --args='{\"format\":\"json\"}'");
        System.out.println("  astra-runner --action=" + getName() + " --args='{\"customParam\":\"test-value\"}'");
        System.out.println();
        System.out.println("Note: This action is disabled by default. To enable, uncomment @Component annotation.");
    }
}