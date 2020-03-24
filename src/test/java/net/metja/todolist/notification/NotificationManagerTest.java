package net.metja.todolist.notification;

import net.metja.todolist.database.DatabaseManager;
import net.metja.todolist.database.bean.Todo;
import net.metja.todolist.database.bean.UserAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Janne Metso @copy; 2020
 * @since 2020-03-23
 */
class NotificationManagerTest {

    private NotificationManager notificationManager;

    @BeforeEach
    public void setUp() {
        this.notificationManager = new NotificationManager();
    }

    @AfterEach
    public void tearDown() {
        this.notificationManager = null;
    }

    @Test
    public void testInit() {
        List<UserAccount> userAccounts = new LinkedList<>();
        UserAccount user = new UserAccount(1, "user", "pwd", Arrays.asList("user"), "user@example.com");
        userAccounts.add(user);

        List<Todo> todos = new LinkedList<>();
        Todo todo = new Todo(1, -1, "One");
        todos.add(todo);
        todo = new Todo(2, -1, "Two");
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.now());
        todo.setDueTime(LocalTime.now());
        todo.setDueTimezone(ZoneOffset.of("+0200"));
        todos.add(todo);
        todo = new Todo(3, -1, "Three");
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.now());
        todos.add(todo);
        todo = new Todo(4, -1, "Four");
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.now().plusDays(1));
        todos.add(todo);
        todo = new Todo(5, -1, "Five");
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.now().plusDays(1));
        todos.add(todo);

        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(databaseManager.getUsers()).thenReturn(userAccounts);
        when(databaseManager.getUserList("user")).thenReturn(1);
        when(databaseManager.getTodos(1)).thenReturn(todos);
        when(databaseManager.updateTodo(1, todos.get(0))).thenReturn(true);
        when(databaseManager.updateTodo(1, todos.get(1))).thenReturn(true);
        when(databaseManager.updateTodo(1, todos.get(2))).thenReturn(true);
        when(databaseManager.updateTodo(1, todos.get(3))).thenReturn(true);
        when(databaseManager.updateTodo(1, todos.get(4))).thenReturn(true);

        assertNotNull(databaseManager, "database manager mock");
        AtomicInteger sentNotifications = new AtomicInteger();

        this.notificationManager.setEmailClient((subject, text, user1) -> {
            assertEquals("user", user1.getUsername(), "Username");
            System.out.println("Booya! "+subject);
            sentNotifications.getAndIncrement();
        });
        this.notificationManager.setDatabaseManager(databaseManager);
        this.notificationManager.init();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(2, sentNotifications.get(), "Notifications");
    }

}