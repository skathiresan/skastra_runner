package com.acme.astra.app.cli.action;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Help action that provides detailed information about available actions.
 */
@Component
public class HelpAction implements CliAction {
    
    private final List<CliAction> actions;
    
    @Autowired
    public HelpAction(List<CliAction> actions) {
        this.actions = actions;
    }
    
    @Override
    public String getName() {
        return "help";
    }
    
    @Override
    public String getDescription() {
        return "Display help information for actions";
    }
    
    @Override
    public int execute(Map<String, String> args) {
        String targetAction = args.get("action");
        
        if (targetAction != null) {
            // Show help for specific action
            Optional<CliAction> action = actions.stream()
                .filter(a -> a.getName().equals(targetAction))
                .findFirst();
                
            if (action.isPresent()) {
                action.get().printUsage();
                return 0;
            } else {
                System.err.println("❌ Unknown action: " + targetAction);
                System.err.println("Available actions: " + getAvailableActionNames());
                return 1;
            }
        } else {
            // Show general help
            printGeneralHelp();
            return 0;
        }
    }
    
    @Override
    public void printUsage() {
        System.out.println("Usage: astra-runner --action=" + getName() + " [--args='{\"action\":\"<action-name>\"}']");
        System.out.println();
        System.out.println("Description:");
        System.out.println("  " + getDescription());
        System.out.println();
        System.out.println("Options:");
        System.out.println("  action=<name>     Show detailed help for specific action");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Show general help");
        System.out.println("  astra-runner --action=" + getName());
        System.out.println();
        System.out.println("  # Show help for specific action");
        System.out.println("  astra-runner --action=" + getName() + " --args='{\"action\":\"trigger-job\"}'");
    }
    
    private void printGeneralHelp() {
        System.out.println("🎯 Astra Runner CLI Actions");
        System.out.println();
        System.out.println("Available Actions:");
        
        actions.stream()
            .sorted((a, b) -> a.getName().compareTo(b.getName()))
            .forEach(action -> {
                System.out.printf("  %-15s %s%n", action.getName(), action.getDescription());
            });
            
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  astra-runner --action=<action-name> [options]");
        System.out.println();
        System.out.println("For detailed help on a specific action:");
        System.out.println("  astra-runner --action=help --args='{\"action\":\"<action-name>\"}'");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  astra-runner --action=system-info");
        System.out.println("  astra-runner --action=trigger-job --args='{\"parallel\":\"true\"}'");
        System.out.println("  astra-runner --action=help --args='{\"action\":\"trigger-job\"}'");
    }
    
    private String getAvailableActionNames() {
        return actions.stream()
            .map(CliAction::getName)
            .sorted()
            .toList()
            .toString();
    }
}