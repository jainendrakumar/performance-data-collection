package com.example.qlogserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the QLog Server application.
 * Enables Spring Boot auto-configuration and asynchronous processing.
 */
@SpringBootApplication
@EnableAsync // Enable asynchronous processing for @Async methods (like AgentLogProcessingService)
public class ServerApplication {

    /**
     * Main method to run the Spring Boot application.
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

}

