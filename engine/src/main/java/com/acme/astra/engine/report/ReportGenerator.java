package com.acme.astra.engine.report;

import com.acme.astra.model.RunSummary;
import com.acme.spi.TaskResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates standardized reports and summaries.
 */
@Component
public class ReportGenerator {
    
    private static final Logger log = LoggerFactory.getLogger(ReportGenerator.class);
    
    private final ObjectMapper objectMapper;
    
    public ReportGenerator() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Generate all standard reports for a run.
     */
    public void generateReports(RunSummary summary, TaskResult result, Path reportsDir) throws Exception {
        log.info("Generating reports for execution {}", summary.getExecutionId());
        
        // Generate JSON summary
        generateJsonSummary(summary, reportsDir);
        
        // Generate JUnit XML
        generateJunitXml(summary, result, reportsDir);
        
        // Generate HTML summary
        generateHtmlSummary(summary, result, reportsDir);
        
        // Copy/standardize result files
        standardizeResultFiles(result, reportsDir);
        
        log.info("Reports generated successfully in {}", reportsDir);
    }
    
    private void generateJsonSummary(RunSummary summary, Path reportsDir) throws Exception {
        Path summaryFile = reportsDir.resolve("run-summary.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(summaryFile.toFile(), summary);
        log.debug("Generated JSON summary: {}", summaryFile);
    }
    
    private void generateJunitXml(RunSummary summary, TaskResult result, Path reportsDir) throws Exception {
        Path junitFile = reportsDir.resolve("junit.xml");
        
        String status = result.getStatus().name().toLowerCase();
        String testName = summary.getConfig().getGroupArtifact().replace(":", ".");
        long durationSec = result.getDurationMs() != null ? result.getDurationMs() / 1000 : 0;
        
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<testsuite name=\"").append(testName).append("\" ");
        xml.append("tests=\"1\" ");
        xml.append("failures=\"").append("FAILURE".equalsIgnoreCase(status) ? "1" : "0").append("\" ");
        xml.append("errors=\"0\" ");
        xml.append("time=\"").append(durationSec).append("\">\n");
        
        xml.append("  <testcase name=\"execute\" classname=\"").append(testName).append("\" ");
        xml.append("time=\"").append(durationSec).append("\"");
        
        if ("FAILURE".equalsIgnoreCase(status)) {
            xml.append(">\n");
            xml.append("    <failure message=\"").append(escapeXml(result.getMessage())).append("\">");
            if (result.getErrors() != null) {
                for (String error : result.getErrors()) {
                    xml.append(escapeXml(error)).append("\n");
                }
            }
            xml.append("</failure>\n");
            xml.append("  </testcase>\n");
        } else {
            xml.append("/>\n");
        }
        
        xml.append("</testsuite>\n");
        
        Files.writeString(junitFile, xml.toString());
        log.debug("Generated JUnit XML: {}", junitFile);
    }
    
    private void generateHtmlSummary(RunSummary summary, TaskResult result, Path reportsDir) throws Exception {
        Path htmlFile = reportsDir.resolve("summary.html");
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<title>Execution Summary - ").append(summary.getExecutionId()).append("</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("table { border-collapse: collapse; width: 100%; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("th { background-color: #f2f2f2; }\n");
        html.append(".success { color: green; }\n");
        html.append(".failure { color: red; }\n");
        html.append(".info { background-color: #f9f9f9; padding: 10px; margin: 10px 0; }\n");
        html.append("</style>\n</head>\n<body>\n");
        
        html.append("<h1>Execution Summary</h1>\n");
        html.append("<div class=\"info\">\n");
        html.append("<strong>Execution ID:</strong> ").append(summary.getExecutionId()).append("<br>\n");
        html.append("<strong>Timestamp:</strong> ").append(formatTimestamp(summary.getTimestamp())).append("<br>\n");
        html.append("<strong>Status:</strong> <span class=\"").append(result.getStatus().name().toLowerCase()).append("\">")
            .append(result.getStatus().name()).append("</span><br>\n");
        if (result.getDurationMs() != null) {
            html.append("<strong>Duration:</strong> ").append(result.getDurationMs()).append(" ms<br>\n");
        }
        html.append("</div>\n");
        
        html.append("<h2>Configuration</h2>\n");
        html.append("<table>\n");
        html.append("<tr><th>Property</th><th>Value</th></tr>\n");
        html.append("<tr><td>Group:Artifact</td><td>").append(summary.getConfig().getGroupArtifact()).append("</td></tr>\n");
        html.append("<tr><td>Version</td><td>").append(summary.getConfig().getVersion()).append("</td></tr>\n");
        html.append("<tr><td>Mode</td><td>").append(summary.getConfig().getMode()).append("</td></tr>\n");
        html.append("</table>\n");
        
        if (summary.getResolvedArtifact() != null) {
            html.append("<h2>Resolved Artifact</h2>\n");
            html.append("<table>\n");
            html.append("<tr><th>Property</th><th>Value</th></tr>\n");
            html.append("<tr><td>Resolved Version</td><td>").append(summary.getResolvedArtifact().getVersion()).append("</td></tr>\n");
            html.append("<tr><td>SHA256</td><td>").append(summary.getResolvedArtifact().getSha256()).append("</td></tr>\n");
            html.append("<tr><td>Path</td><td>").append(summary.getResolvedArtifact().getPath()).append("</td></tr>\n");
            if (summary.getResolvedArtifact().getDependencies() != null) {
                html.append("<tr><td>Dependencies</td><td>").append(summary.getResolvedArtifact().getDependencies().size()).append("</td></tr>\n");
            }
            html.append("</table>\n");
        }
        
        if (result.getErrors() != null && !result.getErrors().isEmpty()) {
            html.append("<h2>Errors</h2>\n");
            html.append("<ul>\n");
            for (String error : result.getErrors()) {
                html.append("<li>").append(escapeHtml(error)).append("</li>\n");
            }
            html.append("</ul>\n");
        }
        
        if (summary.getOutputFiles() != null && !summary.getOutputFiles().isEmpty()) {
            html.append("<h2>Output Files</h2>\n");
            html.append("<ul>\n");
            for (String file : summary.getOutputFiles()) {
                html.append("<li>").append(escapeHtml(file)).append("</li>\n");
            }
            html.append("</ul>\n");
        }
        
        html.append("</body>\n</html>\n");
        
        Files.writeString(htmlFile, html.toString());
        log.debug("Generated HTML summary: {}", htmlFile);
    }
    
    private void standardizeResultFiles(TaskResult result, Path reportsDir) throws Exception {
        // Create results.json with task result
        Path resultsFile = reportsDir.resolve("results.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(resultsFile.toFile(), result);
        
        // Ensure stdout.log and stderr.log exist (even if empty)
        Path stdoutFile = reportsDir.resolve("stdout.log");
        Path stderrFile = reportsDir.resolve("stderr.log");
        
        if (!Files.exists(stdoutFile)) {
            Files.createFile(stdoutFile);
        }
        if (!Files.exists(stderrFile)) {
            Files.createFile(stderrFile);
        }
        
        log.debug("Standardized result files in {}", reportsDir);
    }
    
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
    
    private String formatTimestamp(Instant timestamp) {
        return timestamp != null ? DateTimeFormatter.ISO_INSTANT.format(timestamp) : "unknown";
    }
}