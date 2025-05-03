package com.example.qlogagent;

import com.example.qlogagent.config.AgentConfig;
import com.example.qlogagent.monitor.DirectoryMonitor;
import com.example.qlogagent.service.FileProcessingService;
import com.example.qlogagent.service.HttpService;
import com.example.qlogagent.service.StateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Main entry point for the QLog Agent application.
 * Initializes configuration, services, and starts the directory monitor.
 */
public class AgentApplication {

    private static final Logger log = LoggerFactory.getLogger(AgentApplication.class);

    public static void main(String[] args) {
        log.info("Starting QLog Agent...");

        AgentConfig config = null;
        try {
            // Allow specifying config file path via command line argument (optional)
            String configPath = (args.length > 0) ? args[0] : null;
            config = new AgentConfig(configPath);
        } catch (IOException e) {
            log.error("Failed to initialize agent configuration: {}", e.getMessage());
            System.exit(1); // Exit if config fails
        }

        // Initialize services
        StateService stateService = new StateService(config);
        HttpService httpService = new HttpService(config);
        FileProcessingService fileProcessingService = new FileProcessingService(stateService);
        DirectoryMonitor directoryMonitor = new DirectoryMonitor(config, stateService, fileProcessingService, httpService);

        // Add shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown signal received. Stopping agent gracefully...");
            directoryMonitor.stop();
            log.info("Agent shutdown complete.");
        }));

        // Start monitoring
        try {
            directoryMonitor.start();
            log.info("QLog Agent started successfully. Monitoring directory: {}", config.getMonitorDirectory());
            // Keep the main thread alive (or the application might exit immediately if monitor uses daemon threads)
            // For a simple implementation, we can just let the scheduler thread run.
            // In a more complex app, might use a CountDownLatch or similar.
        } catch (Exception e) {
            log.error("Failed to start directory monitor: {}", e.getMessage(), e);
            // Attempt to stop monitor if it partially started
            directoryMonitor.stop();
            System.exit(1);
        }
    }
}

