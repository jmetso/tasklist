package net.metja.todolist.notification;

import net.metja.todolist.database.bean.UserAccount;

/**
 * @author: Janne Metso @copy; 2020
 * @since: 2020-03-23
 */
public interface NotificationClient {

    void sendNotification(String subject, String text, UserAccount user);

}
