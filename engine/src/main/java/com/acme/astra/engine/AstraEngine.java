package com.acme.astra.engine;

import com.acme.astra.engine.executor.ArtifactExecutor;
import com.acme.astra.engine.report.ReportGenerator;
import com.acme.astra.engine.resolver.ArtifactResolver;
import com.acme.astra.model.ExecutionConfig;
import com.acme.astra.model.RunSummary;
import com.acme.spi.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.UUID;

/**
 * Main orchestration service for artifact execution.
 */
@Service
public class AstraEngine {
    
    private static final Logger log = LoggerFactory.getLogger(AstraEngine.class);
    
    private final ArtifactResolver artifactResolver;
    private final ArtifactExecutor artifactExecutor;
    private final ReportGenerator reportGenerator;
    
    @Autowired
    public AstraEngine(ArtifactResolver artifactResolver, 
                      ArtifactExecutor artifactExecutor,
                      ReportGenerator reportGenerator) {
        this.artifactResolver = artifactResolver;
        this.artifactExecutor = artifactExecutor;
        this.reportGenerator = reportGenerator;
    }
    
    /**
     * Execute artifact with given configuration and generate reports.
     */
    public RunSummary execute(ExecutionConfig config, Path workspaceDir) throws Exception {
        String executionId = UUID.randomUUID().toString();
        log.info("Starting execution {} for {}:{}", executionId, config.getGroupArtifact(), config.getVersion());
        
        // Create reports directory
        Path reportsDir = Paths.get(config.getReportsDir());
        Files.createDirectories(reportsDir);
        
        // Initialize summary
        RunSummary summary = new RunSummary(executionId);
        summary.setConfig(config);
        summary.setTimestamp(Instant.now());
        
        try {
            // Step 1: Resolve artifact
            log.info("Step 1: Resolving artifact");
            RunSummary.ResolvedArtifact resolvedArtifact = artifactResolver.resolve(
                config.getGroupArtifact(), 
                config.getVersion(), 
                workspaceDir
            );
            summary.setResolvedArtifact(resolvedArtifact);
            
            // Step 2: Execute artifact
            log.info("Step 2: Executing artifact");
            TaskResult result = artifactExecutor.execute(resolvedArtifact, config, reportsDir);
            
            // Step 3: Convert TaskResult to ExecutionResult
            RunSummary.ExecutionResult executionResult = new RunSummary.ExecutionResult();
            executionResult.setStatus(result.getStatus().name());
            executionResult.setExitCode(result.getExitCode());
            executionResult.setStartTime(result.getStartTime());
            executionResult.setEndTime(result.getEndTime());
            executionResult.setDurationMs(result.getDurationMs());
            executionResult.setMessage(result.getMessage());
            executionResult.setErrors(result.getErrors());
            
            summary.setExecutionResult(executionResult);
            summary.setOutputFiles(result.getOutputFiles());
            
            // Step 4: Generate reports
            log.info("Step 3: Generating reports");
            reportGenerator.generateReports(summary, result, reportsDir);
            
            // Set final summary message
            String summaryMsg = String.format("Execution %s completed with status %s in %d ms", 
                executionId, result.getStatus(), result.getDurationMs());
            summary.setSummary(summaryMsg);
            
            log.info("Execution {} completed successfully", executionId);
            return summary;
            
        } catch (Exception e) {
            log.error("Execution {} failed", executionId, e);
            
            // Update summary with failure information
            RunSummary.ExecutionResult executionResult = new RunSummary.ExecutionResult();
            executionResult.setStatus("FAILURE");
            executionResult.setMessage("Execution failed: " + e.getMessage());
            executionResult.setStartTime(summary.getTimestamp());
            executionResult.setEndTime(Instant.now());
            executionResult.setDurationMs(executionResult.getEndTime().toEpochMilli() - 
                                        executionResult.getStartTime().toEpochMilli());
            
            summary.setExecutionResult(executionResult);
            summary.setSummary("Execution failed: " + e.getMessage());
            
            // Try to generate failure report
            try {
                TaskResult failureResult = TaskResult.failure(e.getMessage());
                failureResult.setStartTime(summary.getTimestamp());
                failureResult.setEndTime(Instant.now());
                failureResult.setDurationMs(failureResult.getEndTime().toEpochMilli() - 
                                          failureResult.getStartTime().toEpochMilli());
                
                reportGenerator.generateReports(summary, failureResult, reportsDir);
            } catch (Exception reportException) {
                log.warn("Failed to generate failure reports", reportException);
            }
            
            throw e;
        }
    }
}