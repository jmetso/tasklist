package net.metja.todolist.configuration;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;

/**
 * @author Janne Metso @copy; 2020
 * @since 2020-03-23
 */
public class ConfigUtil {

    public static final String CONFIG_FILE_KEY = "CONFIG_FILE";
    @Value("#{systemProperties['CONFIG_FILE'] ?: 'conf/tasklist.properties'}")
    private String file;

    private static Logger logger = LoggerFactory.getLogger(ConfigUtil.class);
    private PropertiesConfiguration config = null;

    public ConfigUtil() {

    }

    public void init() {
        if(this.file == null) {
            this.file = System.getProperty(CONFIG_FILE_KEY);
        }
        logger.debug("Config file is "+this.file);
        try {
            Configurations configurations = new Configurations();
            FileBasedConfigurationBuilder<PropertiesConfiguration> builder = configurations.propertiesBuilder(new File(this.file));
            this.config = builder.getConfiguration();
        } catch(org.apache.commons.configuration2.ex.ConfigurationException e) {
            logger.error("Unable to read configuration file "+this.file, e);
        }
    }

    public String getSMTPServer() {
        return this.config.getString("smtp.host", null);
    }

    public int getSMTPPort() {
        return this.config.getInt("smtp.port", 587);
    }

    public String getFromEmail() {
        return this.config.getString("smtp.from", null);
    }

    public String getSMTPUsername() {
        return this.config.getString("smtp.username", null);
    }

    public String getSMTPPassword() {
        return this.config.getString("smtp.password", null);
    }

}