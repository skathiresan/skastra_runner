package com.acme.astra.app.cli.action;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * System information action that displays Java version, OS details, and runtime arguments.
 */
@Component
public class SystemInfoAction implements CliAction {
    
    @Override
    public String getName() {
        return "system-info";
    }
    
    @Override
    public String getDescription() {
        return "Display system information (Java version, OS, runtime arguments)";
    }
    
    @Override
    public int execute(Map<String, String> args) {
        try {
            System.out.println("=== Astra Runner System Information ===");
            System.out.println();
            
            // Java Version Information
            System.out.println("🔧 Java Information:");
            System.out.println("   Java Version: " + System.getProperty("java.version"));
            System.out.println("   Java Vendor: " + System.getProperty("java.vendor"));
            System.out.println("   Java Home: " + System.getProperty("java.home"));
            System.out.println("   Java VM: " + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version"));
            System.out.println("   Java Runtime: " + System.getProperty("java.runtime.name") + " " + System.getProperty("java.runtime.version"));
            System.out.println();
            
            // Operating System Information
            System.out.println("💻 Operating System:");
            System.out.println("   OS Name: " + System.getProperty("os.name"));
            System.out.println("   OS Version: " + System.getProperty("os.version"));
            System.out.println("   OS Architecture: " + System.getProperty("os.arch"));
            System.out.println("   Available Processors: " + Runtime.getRuntime().availableProcessors());
            System.out.println();
            
            // Runtime Information  
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            System.out.println("🧠 Memory Information:");
            System.out.println("   Max Memory: " + formatBytes(maxMemory));
            System.out.println("   Total Memory: " + formatBytes(totalMemory));
            System.out.println("   Used Memory: " + formatBytes(usedMemory));
            System.out.println("   Free Memory: " + formatBytes(freeMemory));
            System.out.println();
            
            // User and Environment Information
            System.out.println("👤 User Information:");
            System.out.println("   User Name: " + System.getProperty("user.name"));
            System.out.println("   User Home: " + System.getProperty("user.dir"));
            System.out.println("   Working Directory: " + System.getProperty("user.dir"));
            System.out.println();
            
            // Arguments passed to this action
            System.out.println("📋 Arguments Passed to Action:");
            if (args.isEmpty()) {
                System.out.println("   No arguments provided");
            } else {
                args.forEach((key, value) -> 
                    System.out.println("   " + key + " = " + value));
            }
            System.out.println();
            
            // Astra Runner Information
            System.out.println("🚀 Astra Runner Information:");
            System.out.println("   Version: 1.0.0");
            System.out.println("   Build: " + getBuildInfo());
            System.out.println("   Execution Time: " + java.time.Instant.now());
            
            return 0;
            
        } catch (Exception e) {
            System.err.println("❌ Error gathering system information: " + e.getMessage());
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
        System.out.println("  --verbose=true    Show additional system details");
        System.out.println("  --format=json     Output in JSON format (default: human-readable)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  astra-runner --action=system-info");
        System.out.println("  astra-runner --action=system-info --verbose=true");
        System.out.println("  astra-runner --action=system-info --format=json");
        System.out.println();
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int unit = 1024;
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
    
    private String getBuildInfo() {
        // In a real application, this could read from a build properties file
        // For now, return basic information
        return "Development Build - " + java.time.LocalDate.now();
    }
}