package ru.otus.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Data
public class AppConfig {

    @Getter
    private static Optional<AppConfig> configuration;

    private String host;
    private int port;
    private int nodesPort;
    private int numberOfNodes;

    static {
        ObjectMapper objectMapper = new ObjectMapper();
        ClassLoader classLoader = AppConfig.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream("config.json")) {
            if (inputStream != null) {
                configuration = Optional.of(objectMapper.readValue(inputStream, AppConfig.class));
            } else {
                throw new IOException("config.json not found in the resources directory.");
            }
        } catch (IOException e) {
            System.err.println("Failed to load configuration from config.json");
            e.printStackTrace();
            throw new RuntimeException("Failed to load configuration", e);
        }
    }
}