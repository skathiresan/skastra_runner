package com.example.sample;

import com.acme.spi.Task;
import com.acme.spi.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sample Task implementation for testing Astra Runner.
 */
public class HelloWorldTask implements Task {
    
    private static final Logger log = LoggerFactory.getLogger(HelloWorldTask.class);
    
    @Override
    public TaskResult run(Map<String, String> args, Path outDir) throws Exception {
        log.info("Starting HelloWorldTask execution");
        
        Instant startTime = Instant.now();
        
        // Get parameters
        String name = args.getOrDefault("name", "World");
        String greeting = args.getOrDefault("greeting", "Hello");
        boolean createExtra = Boolean.parseBoolean(args.getOrDefault("createExtra", "false"));
        
        log.info("Parameters: name={}, greeting={}, createExtra={}", name, greeting, createExtra);
        
        // Create main output
        String message = greeting + ", " + name + "!";
        Path outputFile = outDir.resolve("greeting.txt");
        Files.writeString(outputFile, message);
        
        log.info("Created greeting file: {}", outputFile);
        
        // Create additional files if requested
        List<String> outputFiles = new java.util.ArrayList<>();
        outputFiles.add(outputFile.toString());
        
        if (createExtra) {
            // Create a JSON output file
            Path jsonFile = outDir.resolve("data.json");
            String jsonData = String.format("""
                {
                  "message": "%s",
                  "timestamp": "%s",
                  "parameters": {
                    "name": "%s",
                    "greeting": "%s"
                  }
                }
                """, message, startTime, name, greeting);
            Files.writeString(jsonFile, jsonData);
            outputFiles.add(jsonFile.toString());
            
            // Create a report file
            Path reportFile = outDir.resolve("report.txt");
            String report = String.format("""
                Task Execution Report
                ====================
                
                Task: HelloWorldTask
                Execution Time: %s
                Message: %s
                Parameters:
                - name: %s
                - greeting: %s
                - createExtra: %s
                
                Files Created:
                - %s
                - %s
                - %s
                """, startTime, message, name, greeting, createExtra, 
                outputFile.getFileName(), jsonFile.getFileName(), reportFile.getFileName());
            Files.writeString(reportFile, report);
            outputFiles.add(reportFile.toString());
        }
        
        Instant endTime = Instant.now();
        long duration = endTime.toEpochMilli() - startTime.toEpochMilli();
        
        // Create metrics
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("messageLength", message.length());
        metrics.put("filesCreated", outputFiles.size());
        metrics.put("executionDurationMs", duration);
        metrics.put("nameParameter", name);
        metrics.put("greetingParameter", greeting);
        
        // Create successful result
        TaskResult result = TaskResult.success()
            .withOutputFiles(outputFiles)
            .withMetrics(metrics)
            .withTiming(startTime, endTime);
        
        result.setMessage("HelloWorldTask completed successfully with message: " + message);
        
        log.info("HelloWorldTask execution completed in {}ms", duration);
        
        return result;
    }
    
    @Override
    public String getName() {
        return "HelloWorldTask";
    }
    
    @Override 
    public String getVersion() {
        return "1.0.0";
    }
}