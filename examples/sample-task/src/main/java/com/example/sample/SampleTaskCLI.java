package com.example.sample;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * CLI version of the sample task for testing CLI mode execution.
 */
public class SampleTaskCLI {
    
    private static final Logger log = LoggerFactory.getLogger(SampleTaskCLI.class);
    
    public static void main(String[] args) {
        log.info("Starting SampleTaskCLI");
        
        try {
            // Simple argument parsing
            Map<String, String> params = parseArgs(args);
            
            String name = params.getOrDefault("name", "World");
            String greeting = params.getOrDefault("greeting", "Hello");
            String outputDir = params.getOrDefault("outputDir", ".");
            
            log.info("Parameters: name={}, greeting={}, outputDir={}", name, greeting, outputDir);
            
            // Create output directory
            Path outDir = Paths.get(outputDir);
            Files.createDirectories(outDir);
            
            // Execute the task logic
            Instant startTime = Instant.now();
            String message = greeting + ", " + name + "!";
            
            // Create output file
            Path outputFile = outDir.resolve("cli-greeting.txt");
            Files.writeString(outputFile, message);
            
            // Create a simple report
            Path reportFile = outDir.resolve("cli-report.json");
            Map<String, Object> report = new HashMap<>();
            report.put("message", message);
            report.put("timestamp", startTime.toString());
            report.put("outputFile", outputFile.toString());
            report.put("exitCode", 0);
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(reportFile.toFile(), report);
            
            Instant endTime = Instant.now();
            long duration = endTime.toEpochMilli() - startTime.toEpochMilli();
            
            log.info("Task completed successfully in {}ms", duration);
            log.info("Created files: {} and {}", outputFile, reportFile);
            
            // Print to stdout for CLI mode capture
            System.out.println("Task completed successfully!");
            System.out.println("Message: " + message);
            System.out.println("Output file: " + outputFile);
            System.out.println("Duration: " + duration + "ms");
            
            System.exit(0);
            
        } catch (Exception e) {
            log.error("Task execution failed", e);
            System.err.println("Task execution failed: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> params = new HashMap<>();
        
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--") && i + 1 < args.length) {
                String key = args[i].substring(2);
                String value = args[i + 1];
                params.put(key, value);
                i++; // Skip the value argument
            }
        }
        
        return params;
    }
}