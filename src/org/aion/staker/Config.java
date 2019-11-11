package org.aion.staker;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
    private Properties properties;

    public static Config load(String configFilePath) {
        return new Config(configFilePath);
    }

    private Config(String configFilePath) {
        properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream(configFilePath)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read config file " + configFilePath);
        }
    }

    public String getConfigValue(String key) {
        return properties.getProperty(key);
    }


}
