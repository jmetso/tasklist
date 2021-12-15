package net.metja.todolist.configuration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Janne Metso @copy; 2019
 * @since 2020-03-23
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestSecurityConfiguration.class})
public class ConfigUtilTest {

    private ConfigUtil configUtil;

    static {
        System.setProperty(ConfigUtil.CONFIG_FILE_KEY, "test.properties");

    }

    @BeforeEach
    public void setUp() {
        File config = new File("test.properties");
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(config, false));
            bw.write("smtp.host=smtp.example.com");
            bw.newLine();
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert(config.exists());
        this.configUtil = new ConfigUtil();
        this.configUtil.init();

    }

    @AfterEach
    public void tearDown() {
        this.configUtil = null;
        File config = new File("test.properties");
        config.delete();
    }

    @Test
    public void testGetSMTPServer() {
        assertNotNull(this.configUtil, "ConfigUtil");
        assertEquals("smtp.example.com", this.configUtil.getSMTPServer(), "Server");
    }

    @Test
    public void testGetSMTPort() {
        assertNotNull(this.configUtil, "ConfigUtil");
        assertEquals(587, this.configUtil.getSMTPPort(), "Port");
    }

}