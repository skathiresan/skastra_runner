package com.acme.astra.app.rest;

import com.acme.astra.app.cli.action.ActionRegistry;
import com.acme.astra.app.cli.action.ActionRegistry.ActionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API controller for CLI actions.
 * Exposes the CLI action system via HTTP endpoints.
 */
@RestController
@RequestMapping("/api/v1/actions")
public class ActionController {
    
    private static final Logger log = LoggerFactory.getLogger(ActionController.class);
    
    private final ActionRegistry actionRegistry;
    
    @Autowired
    public ActionController(ActionRegistry actionRegistry) {
        this.actionRegistry = actionRegistry;
    }
    
    /**
     * Get list of available actions.
     */
    @GetMapping
    public ResponseEntity<ActionListResponse> listActions() {
        log.info("Listing available actions");
        
        ActionListResponse response = new ActionListResponse();
        List<ActionRegistry.ActionInfo> actionInfos = actionRegistry.getAvailableActions();
        response.setActions(actionInfos);
        response.setStatus("SUCCESS");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Execute a specific action.
     */
    @PostMapping("/{actionName}")
    public ResponseEntity<ActionExecutionResponse> executeAction(
            @PathVariable String actionName,
            @RequestBody(required = false) Map<String, String> args) {
        
        log.info("Executing action: {} with args: {}", actionName, args);
        
        try {
            int exitCode = actionRegistry.executeAction(actionName, args != null ? args : Map.of());
            
            ActionExecutionResponse response = new ActionExecutionResponse();
            response.setActionName(actionName);
            response.setExitCode(exitCode);
            response.setStatus(exitCode == 0 ? "SUCCESS" : "FAILURE");
            response.setMessage(exitCode == 0 ? "Action completed successfully" : "Action failed");
            
            return exitCode == 0 ? 
                ResponseEntity.ok(response) : 
                ResponseEntity.status(500).body(response);
                
        } catch (Exception e) {
            log.error("Action execution failed", e);
            
            ActionExecutionResponse response = new ActionExecutionResponse();
            response.setActionName(actionName);
            response.setExitCode(-1);
            response.setStatus("ERROR");
            response.setMessage("Action execution failed: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Get help for a specific action.
     */
    @GetMapping("/{actionName}/help")
    public ResponseEntity<ActionHelpResponse> getActionHelp(@PathVariable String actionName) {
        log.info("Getting help for action: {}", actionName);
        
        try {
            // Capture the printUsage output
            java.io.StringWriter stringWriter = new java.io.StringWriter();
            java.io.PrintWriter printWriter = new java.io.PrintWriter(stringWriter);
            
            // Temporarily redirect System.out
            java.io.PrintStream originalOut = System.out;
            System.setOut(new java.io.PrintStream(new java.io.ByteArrayOutputStream()) {
                @Override
                public void println(String x) {
                    printWriter.println(x);
                }
                @Override
                public void print(String s) {
                    printWriter.print(s);
                }
            });
            
            try {
                var action = actionRegistry.findAction(actionName);
                if (action.isPresent()) {
                    action.get().printUsage();
                    
                    ActionHelpResponse response = new ActionHelpResponse();
                    response.setActionName(actionName);
                    response.setDescription(action.get().getDescription());
                    response.setUsage(stringWriter.toString());
                    response.setStatus("SUCCESS");
                    
                    return ResponseEntity.ok(response);
                } else {
                    ActionHelpResponse response = new ActionHelpResponse();
                    response.setActionName(actionName);
                    response.setStatus("NOT_FOUND");
                    response.setMessage("Action not found: " + actionName);
                    
                    return ResponseEntity.status(404).body(response);
                }
            } finally {
                System.setOut(originalOut);
                printWriter.close();
            }
            
        } catch (Exception e) {
            log.error("Failed to get action help", e);
            
            ActionHelpResponse response = new ActionHelpResponse();
            response.setActionName(actionName);
            response.setStatus("ERROR");
            response.setMessage("Failed to get help: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
    
    // Response DTOs
    public static class ActionListResponse {
        private List<ActionRegistry.ActionInfo> actions;
        private String status;
        
        // Getters and setters
        public List<ActionRegistry.ActionInfo> getActions() { return actions; }
        public void setActions(List<ActionRegistry.ActionInfo> actions) { this.actions = actions; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    public static class ActionExecutionResponse {
        private String actionName;
        private int exitCode;
        private String status;
        private String message;
        private java.time.Instant timestamp = java.time.Instant.now();
        
        // Getters and setters
        public String getActionName() { return actionName; }
        public void setActionName(String actionName) { this.actionName = actionName; }
        public int getExitCode() { return exitCode; }
        public void setExitCode(int exitCode) { this.exitCode = exitCode; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public java.time.Instant getTimestamp() { return timestamp; }
        public void setTimestamp(java.time.Instant timestamp) { this.timestamp = timestamp; }
    }
    
    public static class ActionHelpResponse {
        private String actionName;
        private String description;
        private String usage;
        private String status;
        private String message;
        
        // Getters and setters
        public String getActionName() { return actionName; }
        public void setActionName(String actionName) { this.actionName = actionName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getUsage() { return usage; }
        public void setUsage(String usage) { this.usage = usage; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}