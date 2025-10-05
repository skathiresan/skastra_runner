package com.acme.astra.engine.resolver;

import com.acme.astra.model.RunSummary.ResolvedArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Resolves artifacts from Artifactory using Gradle's dependency resolution.
 */
@Component
public class ArtifactResolver {
    
    private static final Logger log = LoggerFactory.getLogger(ArtifactResolver.class);
    
    private final String artifactoryUrl;
    private final String artifactoryUser;
    private final String artifactoryPassword;
    
    public ArtifactResolver() {
        this.artifactoryUrl = System.getenv("ARTIFACTORY_URL");
        this.artifactoryUser = System.getenv("ARTIFACTORY_USER");
        this.artifactoryPassword = System.getenv("ARTIFACTORY_PASSWORD");
    }
    
    /**
     * Resolve artifact and its dependencies from Artifactory.
     */
    public ResolvedArtifact resolve(String groupArtifact, String version, Path workDir) throws Exception {
        log.info("Resolving artifact {}:{}", groupArtifact, version);
        
        String[] parts = groupArtifact.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Group:Artifact must be in format 'group:artifact'");
        }
        
        String groupId = parts[0];
        String artifactId = parts[1];
        
        // Create temporary Gradle build for resolution
        Path tempBuildDir = workDir.resolve("temp-gradle-build");
        Files.createDirectories(tempBuildDir);
        
        try {
            String resolvedVersion = resolveVersion(groupId, artifactId, version, tempBuildDir);
            Path artifactPath = downloadArtifact(groupId, artifactId, resolvedVersion, tempBuildDir);
            List<String> dependencies = resolveDependencies(groupId, artifactId, resolvedVersion, tempBuildDir);
            String sha256 = calculateSha256(artifactPath);
            
            ResolvedArtifact resolved = new ResolvedArtifact();
            resolved.setGroupId(groupId);
            resolved.setArtifactId(artifactId);
            resolved.setVersion(resolvedVersion);
            resolved.setPath(artifactPath.toString());
            resolved.setDependencies(dependencies);
            resolved.setSha256(sha256);
            
            log.info("Successfully resolved {}:{}:{} with {} dependencies", 
                groupId, artifactId, resolvedVersion, dependencies.size());
            
            return resolved;
        } finally {
            // Clean up temporary build directory
            deleteRecursively(tempBuildDir);
        }
    }
    
    private String resolveVersion(String groupId, String artifactId, String version, Path buildDir) throws Exception {
        if (!version.equals("latest.release") && !version.contains("+")) {
            return version; // Exact version
        }
        
        // For now, return the version as-is. In a real implementation, you would:
        // 1. Query Artifactory REST API to get available versions
        // 2. Apply version resolution logic (latest.release, semver patterns)
        // 3. Return the resolved version
        
        log.warn("Version resolution not fully implemented - using version as provided: {}", version);
        return version;
    }
    
    private Path downloadArtifact(String groupId, String artifactId, String version, Path buildDir) throws Exception {
        // For demonstration purposes, create a mock JAR file
        // In a real implementation, you would:
        // 1. Construct the Artifactory URL for the artifact
        // 2. Use HTTP client to download the JAR file
        // 3. Verify checksum and authenticity
        
        log.warn("Artifact download not fully implemented - creating mock JAR for demo");
        
        Path artifactPath = buildDir.resolve(artifactId + "-" + version + ".jar");
        
        // Create a simple JAR file for demonstration
        createMockJar(artifactPath, groupId, artifactId, version);
        
        return artifactPath;
    }
    
    private void createMockJar(Path jarPath, String groupId, String artifactId, String version) throws Exception {
        // Create a minimal JAR file with a manifest
        java.util.jar.Manifest manifest = new java.util.jar.Manifest();
        manifest.getMainAttributes().put(java.util.jar.Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(new java.util.jar.Attributes.Name("Implementation-Title"), artifactId);
        manifest.getMainAttributes().put(new java.util.jar.Attributes.Name("Implementation-Version"), version);
        manifest.getMainAttributes().put(new java.util.jar.Attributes.Name("Implementation-Vendor"), groupId);
        
        try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(
                Files.newOutputStream(jarPath), manifest)) {
            // Add a simple text file to make it a valid JAR
            jos.putNextEntry(new java.util.jar.JarEntry("README.txt"));
            jos.write(("Mock JAR for " + groupId + ":" + artifactId + ":" + version).getBytes());
            jos.closeEntry();
        }
        
        log.info("Created mock JAR: {}", jarPath);
    }
    
    private List<String> resolveDependencies(String groupId, String artifactId, String version, Path buildDir) throws Exception {
        // For demonstration purposes, return some mock dependencies
        // In a real implementation, you would:
        // 1. Parse the POM file or query Artifactory for dependency metadata
        // 2. Recursively resolve transitive dependencies
        // 3. Handle version conflicts and exclusions
        
        log.warn("Dependency resolution not fully implemented - returning mock dependencies");
        
        List<String> dependencies = new ArrayList<>();
        dependencies.add("org.slf4j:slf4j-api:2.0.9");
        dependencies.add("com.fasterxml.jackson.core:jackson-core:2.16.0");
        
        return dependencies;
    }
    

    
    private String calculateSha256(Path filePath) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(filePath);
        byte[] hash = digest.digest(fileBytes);
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
    
    private void deleteRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted((a, b) -> b.compareTo(a)) // Reverse order for deletion
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        log.warn("Failed to delete {}: {}", p, e.getMessage());
                    }
                });
        }
    }
}