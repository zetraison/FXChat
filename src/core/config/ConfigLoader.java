package core.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {

    public ConfigLoader() { }

    public static Properties load() {
        FileInputStream fis;
        Properties property = new Properties();
        try {
            fis = new FileInputStream("src/resources/config.properties");
            property.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return property;
    }
}
