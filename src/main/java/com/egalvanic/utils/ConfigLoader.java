package com.egalvanic.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration Loader - Load configuration from properties file
 * Centralizes loading of all configuration values from config.properties
 */
public class ConfigLoader {
    
    private static Properties properties = new Properties();
    private static boolean loaded = false;
    
    static {
        loadProperties();
    }
    
    /**
     * Load properties from config.properties file
     */
    private static void loadProperties() {
        if (!loaded) {
            try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("config/config.properties")) {
                if (input == null) {
                    System.err.println("❌ config.properties file not found in classpath");
                    // Try loading from file system as fallback
                    loadPropertiesFromFile();
                    return;
                }
                properties.load(input);
                loaded = true;
                System.out.println("✅ Configuration loaded from config.properties");
            } catch (IOException ex) {
                System.err.println("❌ Could not load config.properties: " + ex.getMessage());
                // Try loading from file system as fallback
                loadPropertiesFromFile();
            }
        }
    }
    
    /**
     * Load properties from file system as fallback
     */
    private static void loadPropertiesFromFile() {
        try (InputStream input = java.nio.file.Files.newInputStream(
                java.nio.file.Paths.get("config/config.properties"))) {
            properties.load(input);
            loaded = true;
            System.out.println("✅ Configuration loaded from file system config.properties");
        } catch (IOException ex) {
            System.err.println("❌ Could not load config.properties from file system: " + ex.getMessage());
            System.err.println("⚠️ Using default values for configuration");
        }
    }
    
    /**
     * Get property value with default fallback
     */
    public static String getProperty(String key, String defaultValue) {
        String envValue = System.getenv(key.toUpperCase().replace(".", "_"));
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue;
        }
        String propValue = properties.getProperty(key);
        return (propValue != null && !propValue.trim().isEmpty()) ? propValue : defaultValue;
    }
    
    /**
     * Get integer property value with default fallback
     */
    public static int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("⚠️ Invalid integer value for property '" + key + "': " + value + ", using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Get boolean property value with default fallback
     */
    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }
    
    /**
     * Get property value without default (returns null if not found)
     */
    public static String getProperty(String key) {
        String envValue = System.getenv(key.toUpperCase().replace(".", "_"));
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue;
        }
        String propValue = properties.getProperty(key);
        return (propValue != null && !propValue.trim().isEmpty()) ? propValue : null;
    }
}