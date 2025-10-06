# GitHub Copilot Instructions for Astra Runner

## Project Overview

Astra Runner is a comprehensive Java application execution platform with multiple execution modes and interfaces. When working on this project, follow these guidelines to ensure consistency and maintain functionality across all components.

## Architecture Understanding

### Core Execution Modes
- **CLI Mode**: Direct JAR execution via command line
- **SPI Mode**: Service Provider Interface for programmatic execution
- **REST API Mode**: HTTP endpoints for remote execution
- **Action Mode**: Utility actions for system operations and diagnostics

### Key Components
- **Engine**: Core execution logic (`AstraEngine`, `ArtifactExecutor`)
- **Resolver**: Artifact resolution from repositories (`ArtifactResolver`)
- **CLI Interface**: Command line handling (`AstraRunnerCLI`)
- **Actions System**: Extensible utility actions (`CliAction`, `ActionRegistry`)
- **REST API**: HTTP endpoints (`ExecutionController`, `ActionController`)
- **SPI Contracts**: Task interface and DTOs (`Task`, `TaskResult`, `ActionContext`)

## Feature Development Guidelines

### 1. Multi-Mode Integration Requirement

**CRITICAL**: Any new feature must be considered for integration across ALL execution modes:

#### When adding new functionality:
1. **CLI Mode**: Ensure feature works with traditional `--ga --version --mode` execution
2. **SPI Mode**: Consider if Task implementations need access to the feature
3. **REST API**: Evaluate if feature should be exposed via HTTP endpoints
4. **Action Mode**: Determine if feature warrants a new CLI action

#### Integration Checklist:
- [ ] Feature works in CLI mode without breaking existing workflows
- [ ] SPI mode maintains backward compatibility with existing Task implementations
- [ ] REST API endpoints updated if feature provides remote-accessible functionality
- [ ] New CLI actions created if feature provides utility value
- [ ] Parameter forwarding works correctly across all modes
- [ ] JSON/argument parsing handles new parameters properly

### 2. Comprehensive Testing Requirements

**MANDATORY**: All feature additions must be tested across all modes:

#### Testing Protocol:
1. **CLI Action Testing**:
   ```bash
   # Test new action
   java -jar astra-runner.jar --action=new-action
   
   # Test with parameters
   java -jar astra-runner.jar --action=new-action --args='{"param":"value"}'
   
   # Test help system
   java -jar astra-runner.jar --action=help --args='{"action":"new-action"}'
   ```

2. **Traditional CLI Mode Testing**:
   ```bash
   # Test CLI mode still works
   java -jar astra-runner.jar --ga=com.example:test --version=1.0.0 --mode=CLI
   
   # Test with new parameters
   java -jar astra-runner.jar --ga=com.example:test --version=1.0.0 --mode=CLI --newParam=value
   ```

3. **SPI Mode Testing**:
   ```bash
   # Test SPI mode unchanged
   java -jar astra-runner.jar --ga=com.example:test --version=1.0.0 --mode=SPI
   
   # Test ActionContext integration if applicable
   ```

4. **REST API Testing**:
   ```bash
   # Test existing endpoints
   curl http://localhost:8080/api/v1/health
   
   # Test new endpoints if added
   curl -X POST http://localhost:8080/api/v1/actions/new-action
   ```

5. **Default Behavior Testing**:
   ```bash
   # Test no parameters behavior
   java -jar astra-runner.jar
   
   # Test help system
   java -jar astra-runner.jar --action=help
   ```

### 3. Backward Compatibility Rules

#### Non-Negotiable Requirements:
- **Existing CLI commands must continue working unchanged**
- **Task interface implementations must remain compatible**
- **REST API endpoints must maintain existing contracts**
- **Configuration file formats must be backward compatible**
- **Output formats must preserve existing structure (can add fields, cannot remove)**

#### Safe Changes:
- ✅ Adding new CLI actions
- ✅ Adding new REST endpoints
- ✅ Adding optional parameters to existing interfaces
- ✅ Extending ActionContext with new methods (with default implementations)
- ✅ Adding new output formats while preserving existing ones

#### Unsafe Changes:
- ❌ Modifying existing CLI parameter behavior
- ❌ Changing Task interface signatures without default implementations
- ❌ Removing or renaming REST endpoints
- ❌ Changing JSON response structures (removing fields)
- ❌ Modifying exit codes or error messages that scripts may depend on

### 4. Code Quality Standards

#### Implementation Requirements:
- **Spring Boot Integration**: Use `@Component`, `@Autowired` for dependency injection
- **Error Handling**: Comprehensive try-catch with meaningful error messages
- **Logging**: Use SLF4J with appropriate log levels
- **JSON Support**: Use Jackson ObjectMapper for consistent serialization
- **Documentation**: Update relevant `.md` files with new features

#### Action System Development:
```java
@Component
public class NewAction implements CliAction {
    @Override
    public String getName() { return "new-action"; }
    
    @Override
    public String getDescription() { return "Description of new functionality"; }
    
    @Override
    public int execute(Map<String, String> args) {
        try {
            // Implementation with comprehensive error handling
            return 0; // Success
        } catch (Exception e) {
            log.error("Action failed", e);
            return 1; // Failure
        }
    }
    
    @Override
    public void printUsage() {
        // Comprehensive usage documentation
    }
}
```

### 5. Documentation Requirements

#### Must Update:
- **README.md**: If feature affects main usage patterns
- **CLI_ACTION_SYSTEM.md**: For new actions or action system changes
- **API documentation**: For REST endpoint changes
- **Integration guides**: For cross-mode functionality

#### Documentation Standards:
- Provide working examples for all modes
- Include JSON configuration schemas
- Show error handling and edge cases
- Maintain consistency with existing documentation style

### 6. Testing Validation Checklist

Before considering any feature complete:

#### Functional Testing:
- [ ] All execution modes tested and working
- [ ] Parameter forwarding works correctly
- [ ] Error cases handled gracefully
- [ ] Help system updated and accurate
- [ ] JSON/table/other output formats work as expected

#### Integration Testing:
- [ ] No regression in existing functionality
- [ ] New feature integrates seamlessly
- [ ] Performance impact is acceptable
- [ ] Memory usage remains reasonable

#### Documentation Testing:
- [ ] All examples in documentation work
- [ ] Help output matches documented behavior
- [ ] API contracts match implementation

### 7. Common Pitfalls to Avoid

#### Architecture Violations:
- **Don't bypass the action system** for new CLI utilities
- **Don't hardcode dependencies** - use Spring DI
- **Don't ignore existing patterns** - follow established conventions
- **Don't break the modular structure** - respect package boundaries

#### Testing Oversights:
- **Don't test only the happy path** - test error conditions
- **Don't test only one mode** - verify all execution paths
- **Don't ignore edge cases** - test parameter combinations
- **Don't skip integration testing** - verify system-wide behavior

## Maintaining This Instructions File

### 8. Copilot Instructions Update Requirements

**CRITICAL**: This `.github/copilot-instructions.md` file must be updated whenever significant changes are made to the project:

#### When to Update:
- ✅ **New features added** - Document new capabilities and usage patterns
- ✅ **Architecture changes** - Update component descriptions and relationships
- ✅ **New execution modes** - Add to core execution modes section
- ✅ **API changes** - Update interface descriptions and examples
- ✅ **New modules/packages** - Add to key components section
- ✅ **Testing procedures change** - Update testing protocol examples
- ✅ **Documentation structure changes** - Update documentation requirements

#### What to Update:
1. **Project Overview**: Keep current with new capabilities
2. **Architecture Understanding**: Reflect structural changes
3. **Key Components**: Add new modules and their purposes
4. **Testing Protocol**: Update with new testing requirements
5. **Code Examples**: Ensure examples reflect current best practices
6. **Documentation Requirements**: Update with new docs to maintain

#### Update Process:
```bash
# When making changes, always consider:
1. Does this change affect how developers should approach the codebase?
2. Are there new patterns or conventions to follow?
3. Do the examples still reflect current best practices?
4. Are the testing requirements still complete and accurate?

# Update this file BEFORE committing major changes
git add .github/copilot-instructions.md
git commit -m "docs: Update copilot instructions for [feature/change]"
```

### 9. Current Project State Summary

**Last Updated**: October 5, 2025

#### Current Capabilities:
- ✅ **Multi-mode execution**: CLI, SPI, REST API, Action modes
- ✅ **5 Built-in Actions**: help, info, show-args, system-info, trigger-job
- ✅ **Artifact Resolution**: Mock and real Artifactory integration
- ✅ **Command Execution**: Robust utilities with timeout and error handling
- ✅ **JSON Configuration**: Flexible parameter passing across all modes
- ✅ **Comprehensive Documentation**: Usage guides and integration examples
- ✅ **Spring Boot Integration**: Full dependency injection and web server support
- ✅ **Backward Compatibility**: All existing functionality preserved

#### Active Development Areas:
- **Action System Expansion**: New utility actions being added regularly
- **REST API Enhancement**: Additional endpoints for remote operation
- **SPI Integration**: ActionContext for Task implementations
- **Testing Framework**: Comprehensive cross-mode validation

#### Integration Points:
- **ActionRegistry**: Central hub for action discovery and execution
- **ArtifactResolver**: Pluggable artifact resolution system
- **CommandExecutor**: Reusable command execution utilities
- **Task Interface**: SPI contract with ActionContext enhancement

## Summary

The Astra Runner has evolved into a comprehensive platform with multiple execution modes and interfaces. Any feature development must respect this architecture and ensure compatibility across all modes. The action system provides the primary extension point for new functionality, while the existing CLI, SPI, and REST interfaces must remain stable and functional.

Remember: **Multi-mode compatibility and comprehensive testing are not optional - they are fundamental requirements for maintaining the integrity and usability of the Astra Runner platform.**

**Keep this instructions file current** - it serves as the definitive guide for AI-assisted development and ensures consistent, high-quality contributions to the codebase.