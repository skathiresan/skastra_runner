package com.acme.astra.app.cli.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Arguments display action that prints all arguments passed to the application.
 */
@Component
public class ArgumentsAction implements CliAction {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String getName() {
        return "show-args";
    }
    
    @Override
    public String getDescription() {
        return "Display all arguments passed to the application";
    }
    
    @Override
    public int execute(Map<String, String> args) {
        try {
            String format = args.getOrDefault("format", "table");
            boolean includeEmpty = "true".equals(args.get("includeEmpty"));
            
            System.out.println("=== Arguments Passed to Astra Runner ===");
            System.out.println();
            
            if (args.isEmpty()) {
                System.out.println("📋 No arguments provided to this action");
                System.out.println();
                return 0;
            }
            
            // Filter out empty values unless requested
            Map<String, String> filteredArgs = includeEmpty ? args : 
                args.entrySet().stream()
                    .filter(entry -> entry.getValue() != null && !entry.getValue().trim().isEmpty())
                    .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
            
            switch (format.toLowerCase()) {
                case "json" -> printJsonFormat(filteredArgs);
                case "env" -> printEnvironmentFormat(filteredArgs);
                case "properties" -> printPropertiesFormat(filteredArgs);
                default -> printTableFormat(filteredArgs);
            }
            
            System.out.println();
            System.out.println("📊 Summary:");
            System.out.println("   Total arguments: " + args.size());
            System.out.println("   Non-empty arguments: " + filteredArgs.size());
            System.out.println("   Format: " + format);
            
            return 0;
            
        } catch (Exception e) {
            System.err.println("❌ Error displaying arguments: " + e.getMessage());
            return 1;
        }
    }
    
    @Override
    public void printUsage() {
        System.out.println("Usage: astra-runner --action=" + getName() + " [options]");
        System.out.println();
        System.out.println("Description:");
        System.out.println("  " + getDescription());
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --format=<format>     Output format: table, json, env, properties (default: table)");
        System.out.println("  --includeEmpty=true   Include arguments with empty values (default: false)");
        System.out.println();
        System.out.println("Output Formats:");
        System.out.println("  table       Human-readable table format");
        System.out.println("  json        JSON format");
        System.out.println("  env         Environment variable format");
        System.out.println("  properties  Java properties format");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  astra-runner --action=show-args --ga=com.example:lib --version=1.0.0");
        System.out.println("  astra-runner --action=show-args --format=json --includeEmpty=true");
        System.out.println("  astra-runner --action=show-args --format=env");
        System.out.println();
    }
    
    private void printTableFormat(Map<String, String> args) {
        System.out.println("📋 Arguments (Table Format):");
        System.out.println();
        
        if (args.isEmpty()) {
            System.out.println("   No arguments to display");
            return;
        }
        
        // Calculate column widths
        int maxKeyLength = args.keySet().stream().mapToInt(String::length).max().orElse(10);
        int maxValueLength = args.values().stream().mapToInt(String::length).max().orElse(10);
        
        maxKeyLength = Math.max(maxKeyLength, "Argument".length());
        maxValueLength = Math.max(maxValueLength, "Value".length());
        
        // Print header
        String format = "   %-" + maxKeyLength + "s | %-" + maxValueLength + "s%n";
        System.out.printf(format, "Argument", "Value");
        System.out.println("   " + "-".repeat(maxKeyLength) + "-+-" + "-".repeat(maxValueLength) + "-");
        
        // Print arguments
        args.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> System.out.printf(format, entry.getKey(), entry.getValue()));
    }
    
    private void printJsonFormat(Map<String, String> args) throws Exception {
        System.out.println("📋 Arguments (JSON Format):");
        System.out.println();
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(args));
    }
    
    private void printEnvironmentFormat(Map<String, String> args) {
        System.out.println("📋 Arguments (Environment Format):");
        System.out.println();
        
        if (args.isEmpty()) {
            System.out.println("   # No arguments to export");
            return;
        }
        
        args.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String key = entry.getKey().toUpperCase().replace("-", "_").replace(".", "_");
                    String value = entry.getValue();
                    // Escape special characters for shell
                    if (value.contains(" ") || value.contains("\"") || value.contains("'")) {
                        value = "\"" + value.replace("\"", "\\\"") + "\"";
                    }
                    System.out.println("export " + key + "=" + value);
                });
    }
    
    private void printPropertiesFormat(Map<String, String> args) {
        System.out.println("📋 Arguments (Properties Format):");
        System.out.println();
        
        if (args.isEmpty()) {
            System.out.println("   # No arguments to display");
            return;
        }
        
        args.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String value = entry.getValue();
                    // Escape special characters for properties
                    value = value.replace("\\", "\\\\")
                                 .replace("=", "\\=")
                                 .replace(":", "\\:")
                                 .replace("\n", "\\n")
                                 .replace("\r", "\\r")
                                 .replace("\t", "\\t");
                    System.out.println(entry.getKey() + "=" + value);
                });
    }
}