package net.metja.todolist.notification;

import net.metja.todolist.database.DatabaseManager;
import net.metja.todolist.database.bean.Todo;
import net.metja.todolist.database.bean.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.LocalTime;
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
public class NotificationManager {

    @Value("${ENABLE_EMAIL_NOTIFICATIONS:false}")
    private boolean enableEmailNotifications;

    private NotificationClient emailClient;
    private DatabaseManager databaseManager;

    private Timer emailNotificationTimer;
    private final static Logger logger = LoggerFactory.getLogger(NotificationManager.class);

    public NotificationManager() {}

    public void init() {
        if(this.enableEmailNotifications && this.emailClient != null) {
            configureEmailNotifications();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Cancelling all timers ...");
                emailNotificationTimer.cancel();
                logger.info("Cancelling all timers done");
            }));
        }
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
                logger.debug("Found "+users.size()+" users.");
                for(UserAccount user: users) {
                    logger.debug("User "+user.getId()+" - "+user.getUsername());
                    int listID = databaseManager.getUserList(user.getUsername());
                    logger.debug("User list id: "+listID);
                    List<Todo> todos = databaseManager.getTodos(listID);
                    logger.debug("Found "+todos.size()+" task items.");
                    for(Todo todo: todos) {
                        logger.debug("Task "+todo.getId()+" - "+todo.getTitle()+" - scheduled: "+todo.isScheduled());
                        if(todo.isScheduled() && !todo.isDone() && user.getEmail() != null) {
                            logger.debug("Task "+todo.getId()+" is scheduled and not done.");
                            if(sendAlert(user, todo, now)) {
                                logger.debug("Updating todo "+todo.getId()+" with last notification date");
                                todo.setLastNotification(OffsetDateTime.now());
                                boolean success = databaseManager.updateTodo(listID, todo);
                                logger.debug("Updated todo: "+success);
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
        logger.debug("Last notification date: "+lastNotificationDate);
        if(this.isOverdue(now, todo) && (lastNotificationDate == null
                || !hasAlreadyBeenNotifiedToday(now, lastNotificationDate))) {
            logger.debug("Sending notification for overdue");
            if(todo.getDescription() != null) {
                this.emailClient.sendNotification("Task " + todo.getTitle() + " is overdue!", "Task " + todo.getTitle() + " is overdue!\n\nDescription: " + todo.getDescription(), user);
            } else {
                this.emailClient.sendNotification("Task " + todo.getTitle() + " is overdue!", "Task " + todo.getTitle() + " is overdue!", user);
            }
            return true;
        } else  if(this.isDueToday(now, todo) && (lastNotificationDate == null
                || !hasAlreadyBeenNotifiedToday(now, lastNotificationDate))) {
            logger.debug("Sending notification for today");
            if(todo.getDescription() != null) {
                this.emailClient.sendNotification("Task " + todo.getTitle() + " is due today!", "Task " + todo.getTitle() + " is due today!\n\n" + todo.getDescription(), user);
            } else {
                this.emailClient.sendNotification("Task " + todo.getTitle() + " is due today!", "Task " + todo.getTitle() + " is due today!", user);
            }
            return true;
        } else if(this.isDueTomorrow(now, todo) && (lastNotificationDate == null
                || !hasAlreadyBeenNotifiedToday(now, lastNotificationDate))) {
            logger.debug("Sending notification for tomorrow");
            if(todo.getDescription() != null) {
                this.emailClient.sendNotification("Task " + todo.getTitle() + " is due tomorrow!", "Task " + todo.getTitle() + " is due tomorrow!\n\n" + todo.getDescription(), user);
            } else {
                this.emailClient.sendNotification("Task " + todo.getTitle() + " is due tomorrow!", "Task " + todo.getTitle() + " is due tomorrow!", user);
            }
            return true;
        } else if(this.isDueWithinSevenDays(now, todo) && (lastNotificationDate == null
                || !hasAlreadyBeenNotifiedThisWeek(now, lastNotificationDate))) {
            logger.debug("Sending notification for next 7 days");
            if(todo.getDescription() != null) {
                this.emailClient.sendNotification("Task " + todo.getTitle() + " is due in next 7 days", "Task " + todo.getTitle() + " is due in next 7 days!\n\n" + todo.getDescription(), user);
            } else {
                this.emailClient.sendNotification("Task " + todo.getTitle() + " is due in next 7 days", "Task " + todo.getTitle() + " is due in next 7 days", user);
            }
            return true;
        }
        return false;
    }

    private boolean hasAlreadyBeenNotifiedThisWeek(final OffsetDateTime now, final OffsetDateTime notificationDate) {
        OffsetDateTime adjusted = now.minusDays(7);
        int result = adjusted.compareTo(notificationDate);
        logger.debug("Has already been notified this week: "+result);
        if(result > 0 ) {
            return false;
        } else {
            return true;
        }
    }

    private boolean hasAlreadyBeenNotifiedToday(final OffsetDateTime now, final OffsetDateTime notificationDate) {
        int result = now.toLocalDate().compareTo(notificationDate.toLocalDate());
        if(result == 0) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isDueWithinSevenDays(final OffsetDateTime now, final Todo todo) {
        if(!todo.isScheduled()) {
            logger.debug("Todo is not scheduled!");
            return false;
        } else {
            LocalDate dueDate = todo.getDueDate();
            ZoneOffset zoneOffset = now.getOffset();
            if(todo.getDueTimezone() != null) {
                zoneOffset = todo.getDueTimezone();
            }
            logger.debug("Now: "+now);
            OffsetDateTime adjustedNow = now.plusSeconds((zoneOffset.get(ChronoField.OFFSET_SECONDS)-now.get(ChronoField.OFFSET_SECONDS)));
            adjustedNow = adjustedNow.plusDays(7);
            logger.debug("Adjusted: "+adjustedNow);
            LocalDate plusSevenDate = LocalDate.of(adjustedNow.getYear(), adjustedNow.getMonth(), adjustedNow.getDayOfMonth());
            int result = plusSevenDate.compareTo(dueDate);
            return result >= 0;
        }
    }

    private boolean isDueTomorrow(final OffsetDateTime now, final Todo todo) {
        if(!todo.isScheduled()) {
            logger.debug("Todo is not scheduled!");
            return false;
        } else {
            LocalDate dueDate = todo.getDueDate();
            ZoneOffset zoneOffset = now.getOffset();
            if(todo.getDueTimezone() != null) {
                zoneOffset = todo.getDueTimezone();
            }
            OffsetDateTime adjustedNow = now.plusSeconds((zoneOffset.get(ChronoField.OFFSET_SECONDS)-now.get(ChronoField.OFFSET_SECONDS)));
            adjustedNow = adjustedNow.plusDays(1);
            LocalDate tomorrowDate = LocalDate.of(adjustedNow.getYear(), adjustedNow.getMonth(), adjustedNow.getDayOfMonth());
            int result = tomorrowDate.compareTo(dueDate);
            return result == 0;
        }
    }

    private boolean isDueToday(final OffsetDateTime now, final Todo todo) {
        if(!todo.isScheduled()) {
            logger.debug("Todo is not scheduled!");
            return false;
        } else {
            LocalDate dueDate = todo.getDueDate();
            ZoneOffset zoneOffset = now.getOffset();
            if(todo.getDueTimezone() != null) {
                 zoneOffset = todo.getDueTimezone();
            }
            OffsetDateTime adjustedNow = now.plusSeconds((zoneOffset.get(ChronoField.OFFSET_SECONDS)-now.get(ChronoField.OFFSET_SECONDS)));
            LocalDate todayDate = LocalDate.of(adjustedNow.getYear(), adjustedNow.getMonth(), adjustedNow.getDayOfMonth());
            int result = todayDate.compareTo(dueDate);
            return result == 0;
        }
    }

    private boolean isOverdue(final OffsetDateTime now, final Todo todo) {
        if(!todo.isScheduled()) {
            logger.debug("Todo is not scheduled!");
            return false;
        } else {
            LocalDate dueDate = todo.getDueDate();
            ZoneOffset zoneOffset = now.getOffset();
            LocalTime dueTime = todo.getDueTime();
            if(dueTime == null) {
                dueTime = LocalTime.of(now.getHour(), now.getMinute(), now.getSecond());
            }
            if(todo.getDueTimezone() != null) {
                zoneOffset = todo.getDueTimezone();
            }
            OffsetDateTime adjustedNow = now.plusSeconds((zoneOffset.get(ChronoField.OFFSET_SECONDS)-now.get(ChronoField.OFFSET_SECONDS)));
            int result = adjustedNow.compareTo(OffsetDateTime.of(dueDate, dueTime, zoneOffset));
            return result > 0;
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

    protected void setEnableEmailNotifications(boolean enableEmailNotifications) {
        this.enableEmailNotifications = enableEmailNotifications;
    }
}
