# TriggerJob Action - JSON Configuration Reference

## Quick Reference

The `trigger-job` action accepts JSON configuration via the `--args` parameter:

```bash
java -jar astra-runner.jar --action=trigger-job --args='<JSON_CONFIG>'
```

## Complete JSON Schema

```json
{
  "parallel": "true|false",              // Execute jobs in parallel (default: false)
  "dryRun": "true|false",                // Download JARs but don't execute (default: false)  
  "format": "human|json",                // Output format (default: human)
  "workspace": "/path/to/workspace",     // Custom workspace directory (optional)
  "timeout": "300",                      // Timeout per job in seconds (default: 300)
  "showArgs": "true|false",              // Display all available arguments (default: false)
  "jobs": [                              // Custom job configuration (optional)
    {
      "groupArtifact": "com.example:my-job",  // Maven coordinates (required)
      "version": "1.0.0",                     // Version (required, supports latest.release)
      "args": ["--param1=value1"]             // JAR execution arguments (required)
    }
  ]
}
```

## Parameter Details

### Top-Level Options

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `parallel` | String | `"false"` | Execute jobs simultaneously (`"true"`) or one-by-one (`"false"`) |
| `dryRun` | String | `"false"` | Download JARs but skip execution for testing |
| `format` | String | `"human"` | Output format: `"human"` (readable) or `"json"` (structured) |
| `workspace` | String | auto | Custom workspace directory path |
| `timeout` | String | `"300"` | Timeout per job execution in seconds |
| `showArgs` | String | `"false"` | Display all Astra Runner CLI arguments before execution |
| `jobs` | Array | default | Custom job configuration (overrides built-in jobs) |

### Job Configuration

Each job object in the `jobs` array:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `groupArtifact` | String | ✅ | Maven coordinates: `groupId:artifactId` |
| `version` | String | ✅ | Artifact version (exact version or `latest.release`) |
| `args` | Array of Strings | ✅ | Command line arguments passed to the JAR |

## Common Usage Patterns

### 1. Basic Execution
```bash
# Use default jobs, sequential execution
java -jar astra-runner.jar --action=trigger-job
```

### 2. Parallel Execution
```bash
# Execute all jobs in parallel
java -jar astra-runner.jar --action=trigger-job --args='{"parallel":"true"}'
```

### 3. Testing Configuration
```bash
# Dry run with JSON output
java -jar astra-runner.jar --action=trigger-job --args='{"dryRun":"true","format":"json"}'
```

### 4. Single Custom Job
```bash
java -jar astra-runner.jar --action=trigger-job --args='{
  "jobs": [{
    "groupArtifact": "com.mycompany:data-processor",
    "version": "2.1.0",
    "args": ["--config=/app/config.yml", "--mode=production"]
  }]
}'
```

### 5. Multiple Custom Jobs
```bash
java -jar astra-runner.jar --action=trigger-job --args='{
  "parallel": "true",
  "format": "json",
  "jobs": [
    {
      "groupArtifact": "com.example:etl-job",
      "version": "3.0.0", 
      "args": ["--source=database", "--target=warehouse"]
    },
    {
      "groupArtifact": "com.example:cleanup-job",
      "version": "latest.release",
      "args": ["--older-than=7d", "--dry-run=false"]
    }
  ]
}'
```

### 6. With Astra Runner Parameters
```bash
java -jar astra-runner.jar \
  --action=trigger-job \
  --ga=com.parent:orchestrator \
  --version=1.0.0 \
  --jvmArgs="-Xmx2g,-Denv=prod" \
  --args='{"parallel":"true","showArgs":"true"}'
```

## Default Job Configuration

If no custom `jobs` are specified, these default jobs are executed:

```json
{
  "jobs": [
    {
      "groupArtifact": "com.example:data-processor",
      "version": "latest.release",
      "args": ["--mode=batch", "--input=/data/input", "--output=/data/output"]
    },
    {
      "groupArtifact": "com.example:notification-service",
      "version": "1.0.0", 
      "args": ["--type=email", "--template=job-completion"]
    },
    {
      "groupArtifact": "com.example:cleanup-utility",
      "version": "latest.release",
      "args": ["--target=/tmp", "--age=7d", "--dry-run=false"]
    }
  ]
}
```

## JSON Output Format

When `"format": "json"` is specified, the action returns:

```json
{
  "summary": {
    "workspace": "/path/to/workspace",
    "totalJobs": 3,
    "successfulJobs": 2,
    "failedJobs": 1,
    "startTime": "2025-10-05T23:41:29.808782Z",
    "endTime": "2025-10-05T23:41:32.156443Z", 
    "totalDurationMs": 2348
  },
  "results": [
    {
      "groupArtifact": "com.example:data-processor",
      "version": "latest.release",
      "jarPath": "./workspace/job-com.example_data-processor/data-processor-latest.release.jar",
      "args": ["--mode=batch", "--input=/data/input", "--output=/data/output"],
      "success": true,
      "exitCode": 0,
      "message": "Execution completed successfully",
      "startTime": "2025-10-05T23:41:29.811252Z",
      "endTime": "2025-10-05T23:41:31.156443Z",
      "durationMs": 1345
    }
  ]
}
```

## Error Handling

- **Invalid JSON**: Parse errors with specific details
- **Missing required fields**: Validation errors before execution  
- **Execution failures**: Per-job error details in results
- **Timeout**: Jobs exceeding timeout limit are terminated

## Getting Help

```bash
# General help
java -jar astra-runner.jar --action=help

# Specific help for trigger-job
java -jar astra-runner.jar --action=help --args='{"action":"trigger-job"}'

# Show all available arguments
java -jar astra-runner.jar --action=trigger-job --args='{"showArgs":"true"}'
```