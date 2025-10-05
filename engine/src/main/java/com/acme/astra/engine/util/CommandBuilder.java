package com.acme.astra.engine.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Utility class providing common command patterns and builders.
 * Helps create standardized commands for common operations like scripts, database operations, etc.
 */
public class CommandBuilder {
    
    /**
     * Create a shell script execution command.
     */
    public static CommandExecutor shell(String script) {
        return CommandExecutor.of("sh", "-c", script);
    }
    
    /**
     * Create a bash script execution command.
     */
    public static CommandExecutor bash(String script) {
        return CommandExecutor.of("bash", "-c", script);
    }
    
    /**
     * Create a PowerShell script execution command (Windows).
     */
    public static CommandExecutor powershell(String script) {
        return CommandExecutor.of("powershell", "-Command", script);
    }
    
    /**
     * Create a Python script execution command.
     */
    public static CommandExecutor python(String script) {
        return CommandExecutor.of("python", "-c", script);
    }
    
    /**
     * Create a Python file execution command.
     */
    public static CommandExecutor pythonFile(Path scriptFile) {
        return CommandExecutor.of("python", scriptFile.toString());
    }
    
    /**
     * Create a Docker container execution command.
     */
    public static DockerCommandBuilder docker() {
        return new DockerCommandBuilder();
    }
    
    /**
     * Create a MySQL command execution.
     */
    public static DatabaseCommandBuilder mysql() {
        return new DatabaseCommandBuilder("mysql");
    }
    
    /**
     * Create a PostgreSQL command execution.
     */
    public static DatabaseCommandBuilder postgresql() {
        return new DatabaseCommandBuilder("psql");
    }
    
    /**
     * Create a curl HTTP request command.
     */
    public static HttpCommandBuilder curl() {
        return new HttpCommandBuilder();
    }
    
    /**
     * Create a Git command.
     */
    public static GitCommandBuilder git() {
        return new GitCommandBuilder();
    }
    
    /**
     * Create a Maven command.
     */
    public static CommandExecutor maven(String... goals) {
        List<String> command = new ArrayList<>();
        command.add("mvn");
        command.addAll(Arrays.asList(goals));
        return CommandExecutor.of(command);
    }
    
    /**
     * Create a Gradle command.
     */
    public static CommandExecutor gradle(String... tasks) {
        List<String> command = new ArrayList<>();
        command.add("gradle");
        command.addAll(Arrays.asList(tasks));
        return CommandExecutor.of(command);
    }
    
    /**
     * Builder for Docker commands.
     */
    public static class DockerCommandBuilder {
        private final List<String> command = new ArrayList<>();
        
        public DockerCommandBuilder() {
            command.add("docker");
        }
        
        public DockerCommandBuilder run() {
            command.add("run");
            return this;
        }
        
        public DockerCommandBuilder exec() {
            command.add("exec");
            return this;
        }
        
        public DockerCommandBuilder removeAfter() {
            command.add("--rm");
            return this;
        }
        
        public DockerCommandBuilder interactive() {
            command.add("-it");
            return this;
        }
        
        public DockerCommandBuilder volume(String hostPath, String containerPath) {
            command.add("-v");
            command.add(hostPath + ":" + containerPath);
            return this;
        }
        
        public DockerCommandBuilder env(String key, String value) {
            command.add("-e");
            command.add(key + "=" + value);
            return this;
        }
        
        public DockerCommandBuilder port(int hostPort, int containerPort) {
            command.add("-p");
            command.add(hostPort + ":" + containerPort);
            return this;
        }
        
        public DockerCommandBuilder image(String image) {
            command.add(image);
            return this;
        }
        
        public DockerCommandBuilder container(String container) {
            command.add(container);
            return this;
        }
        
        public DockerCommandBuilder args(String... args) {
            command.addAll(Arrays.asList(args));
            return this;
        }
        
        public CommandExecutor build() {
            return CommandExecutor.of(command);
        }
    }
    
    /**
     * Builder for database commands.
     */
    public static class DatabaseCommandBuilder {
        private final List<String> command = new ArrayList<>();
        
        public DatabaseCommandBuilder(String dbCommand) {
            command.add(dbCommand);
        }
        
        public DatabaseCommandBuilder host(String host) {
            if (command.get(0).equals("mysql")) {
                command.add("-h");
                command.add(host);
            } else if (command.get(0).equals("psql")) {
                command.add("-h");
                command.add(host);
            }
            return this;
        }
        
        public DatabaseCommandBuilder port(int port) {
            if (command.get(0).equals("mysql")) {
                command.add("-P");
                command.add(String.valueOf(port));
            } else if (command.get(0).equals("psql")) {
                command.add("-p");
                command.add(String.valueOf(port));
            }
            return this;
        }
        
        public DatabaseCommandBuilder user(String user) {
            command.add("-u");
            command.add(user);
            return this;
        }
        
        public DatabaseCommandBuilder password() {
            command.add("-p");
            return this;
        }
        
        public DatabaseCommandBuilder database(String database) {
            command.add(database);
            return this;
        }
        
        public DatabaseCommandBuilder execute(String sql) {
            command.add("-e");
            command.add(sql);
            return this;
        }
        
        public DatabaseCommandBuilder file(Path sqlFile) {
            if (command.get(0).equals("mysql")) {
                command.add("-e");
                command.add("source " + sqlFile.toString());
            } else if (command.get(0).equals("psql")) {
                command.add("-f");
                command.add(sqlFile.toString());
            }
            return this;
        }
        
        public CommandExecutor build() {
            return CommandExecutor.of(command);
        }
    }
    
    /**
     * Builder for HTTP/curl commands.
     */
    public static class HttpCommandBuilder {
        private final List<String> command = new ArrayList<>();
        
        public HttpCommandBuilder() {
            command.add("curl");
        }
        
        public HttpCommandBuilder url(String url) {
            command.add(url);
            return this;
        }
        
        public HttpCommandBuilder method(String method) {
            command.add("-X");
            command.add(method);
            return this;
        }
        
        public HttpCommandBuilder header(String header) {
            command.add("-H");
            command.add(header);
            return this;
        }
        
        public HttpCommandBuilder data(String data) {
            command.add("-d");
            command.add(data);
            return this;
        }
        
        public HttpCommandBuilder json(String json) {
            command.add("-H");
            command.add("Content-Type: application/json");
            command.add("-d");
            command.add(json);
            return this;
        }
        
        public HttpCommandBuilder output(Path outputFile) {
            command.add("-o");
            command.add(outputFile.toString());
            return this;
        }
        
        public HttpCommandBuilder silent() {
            command.add("-s");
            return this;
        }
        
        public HttpCommandBuilder followRedirects() {
            command.add("-L");
            return this;
        }
        
        public HttpCommandBuilder insecure() {
            command.add("-k");
            return this;
        }
        
        public CommandExecutor build() {
            return CommandExecutor.of(command);
        }
    }
    
    /**
     * Builder for Git commands.
     */
    public static class GitCommandBuilder {
        private final List<String> command = new ArrayList<>();
        
        public GitCommandBuilder() {
            command.add("git");
        }
        
        public GitCommandBuilder clone(String repository) {
            command.add("clone");
            command.add(repository);
            return this;
        }
        
        public GitCommandBuilder pull() {
            command.add("pull");
            return this;
        }
        
        public GitCommandBuilder push() {
            command.add("push");
            return this;
        }
        
        public GitCommandBuilder checkout(String branch) {
            command.add("checkout");
            command.add(branch);
            return this;
        }
        
        public GitCommandBuilder status() {
            command.add("status");
            return this;
        }
        
        public GitCommandBuilder add(String... files) {
            command.add("add");
            command.addAll(Arrays.asList(files));
            return this;
        }
        
        public GitCommandBuilder commit(String message) {
            command.add("commit");
            command.add("-m");
            command.add(message);
            return this;
        }
        
        public GitCommandBuilder branch(String branch) {
            command.add("branch");
            command.add(branch);
            return this;
        }
        
        public GitCommandBuilder remote(String name, String url) {
            command.add("remote");
            command.add("add");
            command.add(name);
            command.add(url);
            return this;
        }
        
        public GitCommandBuilder args(String... args) {
            command.addAll(Arrays.asList(args));
            return this;
        }
        
        public CommandExecutor build() {
            return CommandExecutor.of(command);
        }
    }
}