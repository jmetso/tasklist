package net.metja.todolist;

import net.metja.todolist.configuration.ConfigUtil;
import net.metja.todolist.notification.NotificationClient;
import net.metja.todolist.notification.EmailNotificationClient;
import net.metja.todolist.notification.NotificationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

/**
 * @author Janne Metso &copy; 2019
 * @since 2019-04-21
 */
@Configuration
@SpringBootApplication
public class TodoList {

    @Value("${DB_URL:jdbc:sqlite:db/todolist.db}")
    private String databaseUrl;
    private SingleConnectionDataSource dataSource;
    private static final Logger logger = LoggerFactory.getLogger(TodoList.class);

    @Value("${ENABLE_NOTIFICATIONS:true}")
    private boolean enableNotifications;
    private NotificationClient emailClient;

    private NotificationManager notificationManager;

    private ConfigUtil configUtil;

    public static void main(String... args) {
        SpringApplication.run(TodoList.class, args);
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl impl = new JdbcTokenRepositoryImpl();
        impl.setDataSource(this.jdbcDataSource());
        return impl;
    }

    @Bean
    public synchronized DataSource jdbcDataSource() {
        if(this.dataSource == null) {
            logger.debug("Database url: "+this.databaseUrl);
            if(this.databaseUrl.contains("sqlite")) {
                this.dataSource = new SingleConnectionDataSource();
                this.dataSource.setDriverClassName("org.sqlite.JDBC");
                this.dataSource.setUrl(this.databaseUrl);
            } else {
                logger.error("Unsupported database connection: "+databaseUrl);
                System.exit(1);
            }
        }
        return this.dataSource;
    }

    @Bean
    public synchronized NotificationClient emailNotificationClient() {
        if(this.enableNotifications && this.emailClient == null) {
            logger.debug("Creating email client");
            this.emailClient = new EmailNotificationClient();
        }
        return this.emailClient;
    }

    @Bean(initMethod = "init")
    public synchronized NotificationManager notificationManager() {
        if(this.notificationManager == null && this.enableNotifications) {
            this.notificationManager = new NotificationManager();
        }
        return this.notificationManager;
    }

    @Bean(initMethod = "init")
    public synchronized ConfigUtil configUtil() {
        if(this.configUtil == null) {
            this.configUtil = new ConfigUtil();
        }
        return this.configUtil;
    }

}