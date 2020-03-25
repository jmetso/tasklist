package net.metja.todolist.notification;

import com.sun.mail.smtp.SMTPTransport;
import net.metja.todolist.configuration.ConfigUtil;
import net.metja.todolist.database.bean.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

/**
 * @author Janne Metso @copy; 2020
 * @since 2020-03-23
 */
public class EmailClient implements NotificationClient {

    private ConfigUtil configUtil;
    private static Logger logger = LoggerFactory.getLogger(EmailClient.class);

    public EmailClient() {}

    public void sendNotification(final String SUBJECT, final String MESSAGE, final UserAccount USER) {

        final String SMTP_SERVER = configUtil.getSMTPServer();
        final String USERNAME = configUtil.getSMTPUsername();
        final String PASSWORD = configUtil.getSMTPPassword();

        final String EMAIL_FROM = configUtil.getFromEmail();
        final String EMAIL_TO = USER.getEmail();

        Properties prop = System.getProperties();
        prop.put("mail.smtp.host", SMTP_SERVER); //optional, defined in SMTPTransport
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.port", ""+configUtil.getSMTPPort()); // default port 25
        prop.put("mail.smtp.starttls.enable", "true"); //TLS
        prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        Session session = Session.getInstance(prop, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });
        Message msg = new MimeMessage(session);

        try {

            // from
            msg.setFrom(new InternetAddress(EMAIL_FROM));

            // to
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(EMAIL_TO, false));

            // subject
            msg.setSubject(SUBJECT);

            // content
            msg.setText(MESSAGE);

            msg.setSentDate(new Date());

            // Get SMTPTransport
            SMTPTransport t = (SMTPTransport) session.getTransport("smtps");
            logger.debug("Connecting to email server");
            // connect
            t.connect(SMTP_SERVER, USERNAME, PASSWORD);

            // send
            t.sendMessage(msg, msg.getAllRecipients());

            logger.debug("Response: " + t.getLastServerResponse());

            t.close();

        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @Autowired
    void setConfigUtil(ConfigUtil configUtil) {
        this.configUtil = configUtil;
    }

}