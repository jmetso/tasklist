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

    private List<UserAccount> userAccounts;
    private NotificationManager notificationManager;

    @BeforeEach
    public void setUp() {
        this.notificationManager = new NotificationManager();
        userAccounts = new LinkedList<>();
        UserAccount user = new UserAccount(1, "user", "pwd", Arrays.asList("user"), "user@example.com");
        userAccounts.add(user);
    }

    @AfterEach
    public void tearDown() {
        this.notificationManager = null;
        this.userAccounts = null;
    }

    @Test
    public void testUnscheduledTodo() {
        List<Todo> todos = new LinkedList<>();
        Todo todo = new Todo(1, -1, "One");
        todos.add(todo);

        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(databaseManager.getUsers()).thenReturn(userAccounts);
        when(databaseManager.getUserList("user")).thenReturn(1);
        when(databaseManager.getTodos(1)).thenReturn(todos);
        when(databaseManager.updateTodo(1, todo)).thenReturn(true);

        AtomicInteger sentNotifications = new AtomicInteger();

        this.notificationManager.setEmailClient((subject, text, user1) -> {
            sentNotifications.getAndIncrement();
        });
        this.notificationManager.setDatabaseManager(databaseManager);
        this.notificationManager.init();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(0, sentNotifications.get(), "Notifications");
    }

    @Test
    public void testTaskWithDueDateAndTimeOfToday() {
        List<Todo> todos = new LinkedList<>();
        Todo todo = new Todo(2, -1, "Two");
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.now());
        todo.setDueTime(LocalTime.now());
        todo.setDueTimezone(ZoneOffset.of("+0200"));
        todos.add(todo);

        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(databaseManager.getUsers()).thenReturn(userAccounts);
        when(databaseManager.getUserList("user")).thenReturn(1);
        when(databaseManager.getTodos(1)).thenReturn(todos);
        when(databaseManager.updateTodo(1, todo)).thenReturn(true);

        AtomicInteger sentNotifications = new AtomicInteger();

        this.notificationManager.setEmailClient((subject, text, user1) -> {
            sentNotifications.getAndIncrement();
            assertEquals("user", user1.getUsername(), "Username");
            assertEquals("Task Two is due today!", subject, "Subject");
            assertEquals("Task Two is due today!", text, "Body");
        });
        this.notificationManager.setDatabaseManager(databaseManager);
        this.notificationManager.init();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(1, sentNotifications.get(), "Notifications");
    }

    @Test
    public void testTaskWithDueDateOfToday() {
        List<Todo> todos = new LinkedList<>();
        Todo todo = new Todo(3, -1, "Three");
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.now());
        todos.add(todo);

        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(databaseManager.getUsers()).thenReturn(userAccounts);
        when(databaseManager.getUserList("user")).thenReturn(1);
        when(databaseManager.getTodos(1)).thenReturn(todos);
        when(databaseManager.updateTodo(1, todo)).thenReturn(true);

        AtomicInteger sentNotifications = new AtomicInteger();
        this.notificationManager.setEmailClient((subject, text, user1) -> {
            sentNotifications.getAndIncrement();
            assertEquals("user", user1.getUsername(), "Username");
            assertEquals("Task Three is due today!", subject, "Subject");
            assertEquals("Task Three is due today!", text, "Body");
        });
        this.notificationManager.setDatabaseManager(databaseManager);
        this.notificationManager.init();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(1, sentNotifications.get(), "Notifications");
    }

    @Test
    public void testTaskWithDueDateTomorrow() {
        List<Todo> todos = new LinkedList<>();
        Todo todo = new Todo(4, -1, "Four");
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.now().plusDays(1));
        todos.add(todo);

        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(databaseManager.getUsers()).thenReturn(userAccounts);
        when(databaseManager.getUserList("user")).thenReturn(1);
        when(databaseManager.getTodos(1)).thenReturn(todos);
        when(databaseManager.updateTodo(1, todo)).thenReturn(true);

        AtomicInteger sentNotifications = new AtomicInteger();

        this.notificationManager.setEmailClient((subject, text, user1) -> {
            sentNotifications.getAndIncrement();
            assertEquals("user", user1.getUsername(), "Username");
            assertEquals("Task Four is due tomorrow!", subject, "Subject");
            assertEquals("Task Four is due tomorrow!", text, "Body");
        });
        this.notificationManager.setDatabaseManager(databaseManager);
        this.notificationManager.init();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(1, sentNotifications.get(), "Notifications");
    }

    @Test
    public void testTaskWithDueDateInNextSevenDays() {
        List<Todo> todos = new LinkedList<>();
        Todo todo = new Todo(4, -1, "Five");
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.now().plusDays(7));
        todos.add(todo);

        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(databaseManager.getUsers()).thenReturn(userAccounts);
        when(databaseManager.getUserList("user")).thenReturn(1);
        when(databaseManager.getTodos(1)).thenReturn(todos);
        when(databaseManager.updateTodo(1, todo)).thenReturn(true);

        AtomicInteger sentNotifications = new AtomicInteger();

        this.notificationManager.setEmailClient((subject, text, user1) -> {
            sentNotifications.getAndIncrement();
            assertEquals("user", user1.getUsername(), "Username");
            assertEquals("Task Five is due in next 7 days", subject, "Subject");
            assertEquals("Task Five is due in next 7 days", text, "Body");
        });
        this.notificationManager.setDatabaseManager(databaseManager);
        this.notificationManager.init();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(1, sentNotifications.get(), "Notifications");
    }

    @Test
    public void testTaskWithDueDateInNextEightDays() {
        List<Todo> todos = new LinkedList<>();
        Todo todo = new Todo(4, -1, "Six");
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.now().plusDays(8));
        todos.add(todo);

        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(databaseManager.getUsers()).thenReturn(userAccounts);
        when(databaseManager.getUserList("user")).thenReturn(1);
        when(databaseManager.getTodos(1)).thenReturn(todos);
        when(databaseManager.updateTodo(1, todo)).thenReturn(true);

        AtomicInteger sentNotifications = new AtomicInteger();

        this.notificationManager.setEmailClient((subject, text, user1) -> {
            sentNotifications.getAndIncrement();
        });
        this.notificationManager.setDatabaseManager(databaseManager);
        this.notificationManager.init();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(0, sentNotifications.get(), "Notifications");
    }

    @Test
    public void testTaskWithDueDateInNextTwelveDays() {
        List<Todo> todos = new LinkedList<>();
        Todo todo = new Todo(4, -1, "Seven");
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.now().plusDays(12));
        todos.add(todo);

        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(databaseManager.getUsers()).thenReturn(userAccounts);
        when(databaseManager.getUserList("user")).thenReturn(1);
        when(databaseManager.getTodos(1)).thenReturn(todos);
        when(databaseManager.updateTodo(1, todo)).thenReturn(true);

        AtomicInteger sentNotifications = new AtomicInteger();

        this.notificationManager.setEmailClient((subject, text, user1) -> {
            sentNotifications.getAndIncrement();
        });
        this.notificationManager.setDatabaseManager(databaseManager);
        this.notificationManager.init();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(0, sentNotifications.get(), "Notifications");
    }

    @Test
    public void testTaskWithDueDateOfTodayAndDescription() {
        List<Todo> todos = new LinkedList<>();
        Todo todo = new Todo(3, -1, "Eight");
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.now());
        todo.setDescription("Description.");
        todos.add(todo);

        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(databaseManager.getUsers()).thenReturn(userAccounts);
        when(databaseManager.getUserList("user")).thenReturn(1);
        when(databaseManager.getTodos(1)).thenReturn(todos);
        when(databaseManager.updateTodo(1, todo)).thenReturn(true);

        AtomicInteger sentNotifications = new AtomicInteger();
        this.notificationManager.setEmailClient((subject, text, user1) -> {
            sentNotifications.getAndIncrement();
            assertEquals("user", user1.getUsername(), "Username");
            assertEquals("Task Eight is due today!", subject, "Subject");
            assertEquals("Task Eight is due today!\n\nDescription.", text, "Body");
        });
        this.notificationManager.setDatabaseManager(databaseManager);
        this.notificationManager.init();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(1, sentNotifications.get(), "Notifications");
    }

    @Test
    public void testTaskWithDueDateTodayAndUserWithNoEmail() {
        userAccounts = new LinkedList<>();
        UserAccount user = new UserAccount(1, "user", "pwd", Arrays.asList("user"));
        userAccounts.add(user);

        List<Todo> todos = new LinkedList<>();
        Todo todo = new Todo(3, -1, "Nine");
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.now());
        todos.add(todo);

        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(databaseManager.getUsers()).thenReturn(userAccounts);
        when(databaseManager.getUserList("user")).thenReturn(1);
        when(databaseManager.getTodos(1)).thenReturn(todos);
        when(databaseManager.updateTodo(1, todo)).thenReturn(true);

        AtomicInteger sentNotifications = new AtomicInteger();
        this.notificationManager.setEmailClient((subject, text, user1) -> {
            sentNotifications.getAndIncrement();
            assertEquals("user", user1.getUsername(), "Username");
            assertEquals("Task Three is due today!", subject, "Subject");
            assertEquals("Task Three is due today!", text, "Body");
        });
        this.notificationManager.setDatabaseManager(databaseManager);
        this.notificationManager.init();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(0, sentNotifications.get(), "Notifications");
    }

}