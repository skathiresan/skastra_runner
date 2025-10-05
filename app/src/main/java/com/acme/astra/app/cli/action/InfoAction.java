package com.acme.astra.app.cli.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Combined action that displays both system information and arguments.
 */
@Component
public class InfoAction implements CliAction {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String getName() {
        return "info";
    }
    
    @Override
    public String getDescription() {
        return "Display comprehensive system and argument information";
    }
    
    @Override
    public int execute(Map<String, String> args) {
        try {
            String format = args.getOrDefault("format", "human");
            boolean verbose = "true".equals(args.get("verbose"));
            
            if ("json".equals(format)) {
                return executeJsonFormat(args, verbose);
            } else {
                return executeHumanFormat(args, verbose);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error gathering information: " + e.getMessage());
            return 1;
        }
    }
    
    private int executeHumanFormat(Map<String, String> args, boolean verbose) {
        System.out.println("=== Astra Runner Complete Information ===");
        System.out.println();
        
        // Java and System Information
        printSystemInfo(verbose);
        
        // Arguments Information
        printArgumentsInfo(args);
        
        // Runtime Statistics
        printRuntimeStats();
        
        return 0;
    }
    
    private int executeJsonFormat(Map<String, String> args, boolean verbose) throws Exception {
        Map<String, Object> info = new HashMap<>();
        
        // System information
        Map<String, Object> system = new HashMap<>();
        system.put("java", getJavaInfo());
        system.put("os", getOsInfo());
        system.put("runtime", getRuntimeInfo());
        system.put("user", getUserInfo());
        info.put("system", system);
        
        // Arguments information
        info.put("arguments", args);
        
        // Astra Runner information
        Map<String, Object> astraInfo = new HashMap<>();
        astraInfo.put("version", "1.0.0");
        astraInfo.put("buildInfo", getBuildInfo());
        astraInfo.put("executionTime", java.time.Instant.now().toString());
        info.put("astraRunner", astraInfo);
        
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(info));
        return 0;
    }
    
    private void printSystemInfo(boolean verbose) {
        System.out.println("🔧 Java Information:");
        System.out.println("   Version: " + System.getProperty("java.version"));
        System.out.println("   Vendor: " + System.getProperty("java.vendor"));
        System.out.println("   Home: " + System.getProperty("java.home"));
        
        if (verbose) {
            System.out.println("   VM Name: " + System.getProperty("java.vm.name"));
            System.out.println("   VM Version: " + System.getProperty("java.vm.version"));
            System.out.println("   Runtime Name: " + System.getProperty("java.runtime.name"));
            System.out.println("   Runtime Version: " + System.getProperty("java.runtime.version"));
            System.out.println("   Class Path: " + System.getProperty("java.class.path"));
        }
        System.out.println();
        
        System.out.println("💻 Operating System:");
        System.out.println("   Name: " + System.getProperty("os.name"));
        System.out.println("   Version: " + System.getProperty("os.version"));
        System.out.println("   Architecture: " + System.getProperty("os.arch"));
        System.out.println("   Processors: " + Runtime.getRuntime().availableProcessors());
        System.out.println();
    }
    
    private void printArgumentsInfo(Map<String, String> args) {
        System.out.println("📋 Arguments:");
        if (args.isEmpty()) {
            System.out.println("   No arguments provided");
        } else {
            args.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> 
                        System.out.println("   " + entry.getKey() + " = " + entry.getValue()));
        }
        System.out.println();
    }
    
    private void printRuntimeStats() {
        Runtime runtime = Runtime.getRuntime();
        System.out.println("🧠 Runtime Statistics:");
        System.out.println("   Max Memory: " + formatBytes(runtime.maxMemory()));
        System.out.println("   Total Memory: " + formatBytes(runtime.totalMemory()));
        System.out.println("   Free Memory: " + formatBytes(runtime.freeMemory()));
        System.out.println("   Used Memory: " + formatBytes(runtime.totalMemory() - runtime.freeMemory()));
        System.out.println();
        
        System.out.println("👤 User Information:");
        System.out.println("   Name: " + System.getProperty("user.name"));
        System.out.println("   Home: " + System.getProperty("user.home"));
        System.out.println("   Working Directory: " + System.getProperty("user.dir"));
        System.out.println();
        
        System.out.println("🚀 Astra Runner:");
        System.out.println("   Version: 1.0.0");
        System.out.println("   Build: " + getBuildInfo());
        System.out.println("   Execution Time: " + java.time.Instant.now());
    }
    
    @Override
    public void printUsage() {
        System.out.println("Usage: astra-runner --action=" + getName() + " [options]");
        System.out.println();
        System.out.println("Description:");
        System.out.println("  " + getDescription());
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --format=<format>     Output format: human, json (default: human)");
        System.out.println("  --verbose=true        Show additional system details (default: false)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  astra-runner --action=info");
        System.out.println("  astra-runner --action=info --verbose=true");
        System.out.println("  astra-runner --action=info --format=json");
        System.out.println("  astra-runner --action=info --ga=com.example:lib --version=1.0.0");
        System.out.println();
    }
    
    // Helper methods for JSON format
    private Map<String, String> getJavaInfo() {
        Map<String, String> java = new HashMap<>();
        java.put("version", System.getProperty("java.version"));
        java.put("vendor", System.getProperty("java.vendor"));
        java.put("home", System.getProperty("java.home"));
        java.put("vmName", System.getProperty("java.vm.name"));
        java.put("vmVersion", System.getProperty("java.vm.version"));
        java.put("runtimeName", System.getProperty("java.runtime.name"));
        java.put("runtimeVersion", System.getProperty("java.runtime.version"));
        return java;
    }
    
    private Map<String, String> getOsInfo() {
        Map<String, String> os = new HashMap<>();
        os.put("name", System.getProperty("os.name"));
        os.put("version", System.getProperty("os.version"));
        os.put("architecture", System.getProperty("os.arch"));
        os.put("availableProcessors", String.valueOf(Runtime.getRuntime().availableProcessors()));
        return os;
    }
    
    private Map<String, String> getRuntimeInfo() {
        Runtime runtime = Runtime.getRuntime();
        Map<String, String> runtimeInfo = new HashMap<>();
        runtimeInfo.put("maxMemory", String.valueOf(runtime.maxMemory()));
        runtimeInfo.put("totalMemory", String.valueOf(runtime.totalMemory()));
        runtimeInfo.put("freeMemory", String.valueOf(runtime.freeMemory()));
        runtimeInfo.put("usedMemory", String.valueOf(runtime.totalMemory() - runtime.freeMemory()));
        return runtimeInfo;
    }
    
    private Map<String, String> getUserInfo() {
        Map<String, String> user = new HashMap<>();
        user.put("name", System.getProperty("user.name"));
        user.put("home", System.getProperty("user.home"));
        user.put("workingDirectory", System.getProperty("user.dir"));
        return user;
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int unit = 1024;
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
    
    private String getBuildInfo() {
        return "Development Build - " + java.time.LocalDate.now();
    }
}