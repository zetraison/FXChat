package core.config;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {
    private static final Logger LOGGER = Logger.getLogger(ConfigLoader.class);

    public static Properties load() {
        FileInputStream fis;
        Properties property = new Properties();
        try {
            fis = new FileInputStream("src/main/resources/config.properties");
            property.load(fis);
        } catch (IOException e) {
            LOGGER.error("Config load error" + e);
        }
        return property;
    }
}
