# CLI Actions Integration Test

## Testing SPI and REST API Integration

This document demonstrates the integration between CLI actions and SPI/REST APIs.

### 1. CLI Actions (Already Working)

```bash
# List available actions
java -jar astra-runner.jar --action=help

# Execute system info
java -jar astra-runner.jar --action=system-info

# Execute trigger-job with parallel mode
java -jar astra-runner.jar --action=trigger-job --args='{"parallel":"true","dryRun":"true"}'
```

### 2. REST API Integration (Added)

The following endpoints are now available when running `--rest`:

#### GET /api/v1/actions
Lists all available CLI actions with descriptions.

**Response:**
```json
{
  "status": "SUCCESS",
  "actions": [
    {
      "name": "help",
      "description": "Display help information for actions"
    },
    {
      "name": "info", 
      "description": "Display comprehensive system and argument information"
    },
    {
      "name": "show-args",
      "description": "Display all arguments passed to the application"
    },
    {
      "name": "system-info",
      "description": "Display system information (Java version, OS, runtime arguments)"
    },
    {
      "name": "trigger-job",
      "description": "Download JARs from Artifactory and execute them with predefined parameters"
    }
  ]
}
```

#### POST /api/v1/actions/{actionName}
Executes a specific CLI action via HTTP.

**Example - Execute system-info:**
```bash
curl -X POST http://localhost:8080/api/v1/actions/system-info \
  -H "Content-Type: application/json" \
  -d '{"format":"json"}'
```

**Example - Execute trigger-job:**
```bash
curl -X POST http://localhost:8080/api/v1/actions/trigger-job \
  -H "Content-Type: application/json" \
  -d '{"parallel":"true","dryRun":"true","format":"json"}'
```

#### GET /api/v1/actions/{actionName}/help
Gets detailed help for a specific action.

### 3. SPI Integration (Enhanced)

Task implementations can now access CLI actions through ActionContext:

```java
import com.acme.spi.Task;
import com.acme.spi.TaskResult;
import com.acme.spi.util.ActionContext;

public class EnhancedTask implements Task {
    
    @Override
    public TaskResult run(Map<String, String> args, Path outDir, ActionContext context) throws Exception {
        // Get system information using CLI action
        Map<String, Object> systemInfo = context.getSystemInfo();
        System.out.println("Running on: " + systemInfo.get("os"));
        
        // Execute trigger-job action programmatically
        Map<String, String> jobArgs = Map.of(
            "parallel", "true",
            "dryRun", "true",
            "format", "json"
        );
        int exitCode = context.executeAction("trigger-job", jobArgs);
        
        // Access runtime arguments from parent execution
        Map<String, String> runtimeArgs = context.getRuntimeArguments();
        
        // Check available actions
        List<String> actions = context.getAvailableActions();
        System.out.println("Available actions: " + actions);
        
        return TaskResult.success("Task completed with action integration");
    }
    
    // Backward compatibility - existing tasks still work
    @Override
    public TaskResult run(Map<String, String> args, Path outDir) throws Exception {
        return TaskResult.success("Standard task execution");
    }
}
```

### 4. Integration Benefits

#### Unified Action Access
- **CLI**: `--action=system-info`
- **REST**: `POST /api/v1/actions/system-info`
- **SPI**: `context.executeAction("system-info", args)`

#### Cross-Channel Functionality
- Task implementations can leverage CLI utilities
- REST clients can execute any CLI action remotely
- System information available across all interfaces

#### Backward Compatibility
- Existing Task implementations unchanged
- Existing REST endpoints still work
- CLI interface remains the same

### 5. Architecture Benefits

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   CLI Actions   │◄───│  ActionRegistry  │───►│   REST API      │
│                 │    │                  │    │                 │
│ • system-info   │    │ • Auto-discovery │    │ • HTTP endpoints│
│ • trigger-job   │    │ • Execution      │    │ • JSON responses│
│ • show-args     │    │ • Help system    │    │ • Error handling│
│ • help          │    │                  │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         ▲                        │                        
         │                        ▼                        
┌─────────────────┐    ┌──────────────────┐               
│   SPI Tasks     │    │  ActionContext   │               
│                 │    │                  │               
│ • Enhanced API  │    │ • System info    │               
│ • Action access │    │ • Runtime args   │               
│ • Utilities     │    │ • Action exec    │               
└─────────────────┘    └──────────────────┘               
```

This creates a comprehensive ecosystem where CLI utilities are accessible across all execution modes and interfaces, providing a unified experience for developers and operators.