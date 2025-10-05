# My Command Job

This is your custom command line job implementation that can be executed by Astra Runner.

## 📁 **Where Your Code Goes**

### **For SPI Mode (Task Implementation):**
- **Main Task Code**: `src/main/java/com/example/myjob/MyCommandJobTask.java`
- **Service Registration**: `src/main/resources/META-INF/services/com.acme.spi.Task`

### **For CLI Mode (Standalone Executable):**
- **CLI Main Class**: `src/main/java/com/example/myjob/MyCommandJobCLI.java`

## 🔧 **How to Customize**

### **1. Modify the Task Implementation**

Edit `MyCommandJobTask.java` to add your specific command logic:

```java
@Override
public TaskResult run(Map<String, String> args, Path outDir) throws Exception {
    // YOUR CUSTOM COMMAND LOGIC HERE
    
    // Example: Get command from arguments
    String command = args.get("command");
    
    // Example: Execute your specific commands
    ProcessBuilder pb = new ProcessBuilder("your-command", "arg1", "arg2");
    Process process = pb.start();
    
    // Return success or failure based on your logic
    return TaskResult.success();
}
```

### **2. Add Your Command Logic**

You can implement various types of commands:

**System Commands:**
```java
ProcessBuilder pb = new ProcessBuilder("ls", "-la");
Process process = pb.start();
```

**Script Execution:**
```java
ProcessBuilder pb = new ProcessBuilder("bash", "/path/to/your/script.sh");
Process process = pb.start();
```

**Custom Java Logic:**
```java
// Add your custom Java business logic here
// File processing, data transformation, API calls, etc.
```

## 🚀 **Testing Your Job**

### **Build and Publish:**
```bash
./gradlew :examples:my-command-job:publishToMavenLocal
```

### **Run via Astra Runner (SPI Mode):**
```bash
java -jar app/build/libs/astra-runner.jar \
  --ga="com.example:my-command-job" \
  --version="1.0.0" \
  --mode="SPI" \
  --argsJson='{"command":"echo Hello World","timeout":"60"}' \
  --reportsDir="./my-job-reports"
```

### **Run via Astra Runner (CLI Mode):**
```bash
java -jar app/build/libs/astra-runner.jar \
  --ga="com.example:my-command-job" \
  --version="1.0.0" \
  --mode="CLI" \
  --argsJson='{"command":"echo Hello World","outputDir":"./output"}' \
  --reportsDir="./my-job-reports"
```

## 📋 **Parameters Your Job Accepts**

### **SPI Mode Parameters:**
- `command` - The command to execute (required)
- `workingDir` - Working directory (default: ".")
- `timeout` - Timeout in seconds (default: 300)
- `captureOutput` - Whether to capture stdout/stderr (default: true)

### **CLI Mode Parameters:**
- `command` - The command to execute (required)
- `outputDir` - Output directory for files (default: ".")
- `workingDir` - Working directory (default: ".")
- `timeout` - Timeout in seconds (default: 300)

## 📊 **Output Files Generated**

Your job will generate:
- `command-stdout.log` - Standard output from the command
- `command-stderr.log` - Standard error from the command  
- `execution-report.json` - Detailed execution report
- Standard Astra Runner reports (JSON, HTML, JUnit XML)

## 🔧 **Customization Examples**

### **Example 1: File Processing Job**
```java
String inputFile = args.get("inputFile");
String outputFile = args.get("outputFile");

// Process files
Files.lines(Paths.get(inputFile))
    .map(String::toUpperCase)
    .forEach(line -> Files.write(Paths.get(outputFile), 
                                (line + "\n").getBytes(), 
                                StandardOpenOption.APPEND));
```

### **Example 2: Database Script Runner**
```java
String sqlScript = args.get("sqlScript");
String database = args.get("database");

ProcessBuilder pb = new ProcessBuilder(
    "mysql", "-u", "user", "-p", database, "-e", "source " + sqlScript
);
```

### **Example 3: Docker Container Runner**
```java
String imageName = args.get("image");
String containerArgs = args.getOrDefault("containerArgs", "");

ProcessBuilder pb = new ProcessBuilder(
    "docker", "run", "--rm", imageName, containerArgs
);
```

## 🎯 **Best Practices**

1. **Error Handling**: Always wrap your logic in try-catch blocks
2. **Logging**: Use the SLF4J logger for debugging
3. **Output Files**: Create meaningful output files in the `outDir`
4. **Metrics**: Return useful metrics in the TaskResult
5. **Timeouts**: Respect timeout parameters to prevent hanging
6. **Exit Codes**: Return appropriate exit codes for CLI mode

## 🚀 **Deployment**

Once your job is ready, you can:

1. **Build and publish** to your Artifactory
2. **Use in Jenkins** with the provided pipeline
3. **Execute remotely** via the REST API
4. **Schedule** with your preferred scheduler

Your command job is now ready to be executed by the Astra Runner!