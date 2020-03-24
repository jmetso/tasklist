package net.metja.todolist.notification;

import net.metja.todolist.database.DatabaseManager;
import net.metja.todolist.database.bean.Todo;
import net.metja.todolist.database.bean.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Janne Metso @copy; 2020
 * @since 2020-03-23
 */
@Component
public class NotificationManager {

    private NotificationClient emailClient;
    private DatabaseManager databaseManager;

    private Timer emailNotificationTimer;
    private Logger logger = LoggerFactory.getLogger(NotificationManager.class);

    public NotificationManager() {}

    public void init() {
        if(emailClient != null) {
            configureEmailNotifications();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Cancelling all timers ...");
            emailNotificationTimer.cancel();
            logger.info("Cancelling all timers done");
        }));
    }

    private void configureEmailNotifications() {
        logger.info("Configuring email notifications ...");
        emailNotificationTimer = new Timer("email-timer");
        emailNotificationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.info("Checking notifications ...");
                OffsetDateTime now = OffsetDateTime.now();
                List<UserAccount> users = databaseManager.getUsers();
                logger.info("Found "+users.size()+" users.");
                for(UserAccount user: users) {
                    logger.info("User "+user.getId()+" - "+user.getUsername());
                    int listID = databaseManager.getUserList(user.getUsername());
                    logger.info("User list id: "+listID);
                    List<Todo> todos = databaseManager.getTodos(listID);
                    logger.info("Found "+todos.size()+" task items.");
                    for(Todo todo: todos) {
                        logger.info("Task "+todo.getId()+" - "+todo.getTitle()+" - scheduled: "+todo.isScheduled());
                        if(todo.isScheduled() && !todo.isDone()) {
                            logger.info("Task "+todo.getId()+" is scheduled and not done.");
                            if(sendAlert(user, todo, now)) {
                                logger.info("Updating todo "+todo.getId()+" with last notification date");
                                boolean success = databaseManager.updateTodo(listID, todo);
                                logger.info("Updated todo: "+success);
                            }
                        }
                    }
                }
                logger.info("Checking notifications done");
            }
        }, 0, 3600000);
        logger.info("Configuring email notifications done");
    }

    private boolean sendAlert(final UserAccount user, Todo todo, final OffsetDateTime now) {
        OffsetDateTime lastNotificationDate = todo.getLastNotification();
        logger.info("Last notification date: "+lastNotificationDate);
        if(this.isDueToday(now, todo) && (lastNotificationDate == null
                || !hasAlreadyBeenNotifiedToday(now, lastNotificationDate))) {
            logger.info("Sending notification");
            this.emailClient.sendNotification("Task " + todo.getTitle() + " due.", "Task " + todo.getTitle() + "is due today!\n\n" + todo.getDescription(), user);
            todo.setLastNotification(OffsetDateTime.now());
            return true;
        }
        return false;
    }

    private boolean hasAlreadyBeenNotifiedToday(final OffsetDateTime now, final OffsetDateTime notificationDate) {
        return (notificationDate.getYear() == now.getYear()
                && notificationDate.getMonth() == now.getMonth()
                && notificationDate.getDayOfMonth() == now.getDayOfMonth());
    }

    private boolean isDueToday(final OffsetDateTime now, final Todo todo) {
        if(!todo.isScheduled()) {
            logger.info("Todo is not scheduled!");
            return false;
        } else {
            LocalDate dueDate = todo.getDueDate();
            ZoneOffset zoneOffset = now.getOffset();
            if(todo.getDueTimezone() != null) {
                 zoneOffset = todo.getDueTimezone();
            }
            OffsetDateTime adjustedNow = now.plusSeconds((zoneOffset.get(ChronoField.OFFSET_SECONDS)-now.get(ChronoField.OFFSET_SECONDS)));
            return (adjustedNow.getYear() == dueDate.getYear()
                    && adjustedNow.getMonth() == dueDate.getMonth()
                    && adjustedNow.getDayOfMonth() == dueDate.getDayOfMonth());
        }
    }

    @Autowired
    void setEmailClient(NotificationClient emailClient) {
        this.emailClient = emailClient;
    }

    @Autowired
    void setDatabaseManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

}
