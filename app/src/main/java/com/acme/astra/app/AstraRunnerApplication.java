package com.acme.astra.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.Arrays;

@SpringBootApplication
@ComponentScan(basePackages = {"com.acme.astra"})
public class AstraRunnerApplication {
    
    public static void main(String[] args) {
        // Check if we should run in REST mode or CLI mode
        boolean restMode = args.length == 0 || 
                          Arrays.asList(args).contains("--rest") ||
                          Arrays.asList(args).contains("--server");
        
        SpringApplication app = new SpringApplication(AstraRunnerApplication.class);
        
        if (!restMode) {
            // Disable web server for CLI mode
            app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        }
        
        System.exit(SpringApplication.exit(app.run(args)));
    }
}