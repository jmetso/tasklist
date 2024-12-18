package net.metja.todolist.notification;

import net.metja.todolist.database.DatabaseManager;
import net.metja.todolist.database.bean.Todo;
import net.metja.todolist.database.bean.UserAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
        this.notificationManager.setEnableEmailNotifications(true);
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

        this.notificationManager.setEmailClient((subject, text, user1) -> sentNotifications.getAndIncrement());
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
        todo.setDueTimezone(ZoneOffset.of("+0000"));
        todos.add(todo);

        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(databaseManager.getUsers()).thenReturn(userAccounts);
        when(databaseManager.getUserList("user")).thenReturn(1);
        when(databaseManager.getTodos(1)).thenReturn(todos);
        when(databaseManager.updateTodo(1, todo)).thenReturn(true);

        AtomicInteger sentNotifications = new AtomicInteger();
        AtomicBoolean pass = new AtomicBoolean(false);

        this.notificationManager.setEmailClient((subject, text, user1) -> {
            sentNotifications.getAndIncrement();
            assertEquals("user", user1.getUsername(), "Username");
            assertEquals("Task Two is due today!", subject, "Subject");
            assertEquals("Task Two is due today!", text, "Body");
            pass.set(true);
        });
        this.notificationManager.setDatabaseManager(databaseManager);
        this.notificationManager.init();

        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(1, sentNotifications.get(), "Notifications");
        assertTrue(pass.get(), "Notification content");
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
        AtomicBoolean pass = new AtomicBoolean(false);

        this.notificationManager.setEmailClient((subject, text, user1) -> {
            sentNotifications.getAndIncrement();
            assertEquals("user", user1.getUsername(), "Username");
            assertEquals("Task Three is due today!", subject, "Subject");
            assertEquals("Task Three is due today!", text, "Body");
            pass.set(true);
        });
        this.notificationManager.setDatabaseManager(databaseManager);
        this.notificationManager.init();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(1, sentNotifications.get(), "Notifications");
        assertTrue(pass.get(), "Notification content");
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
        AtomicBoolean pass = new AtomicBoolean(false);

        this.notificationManager.setEmailClient((subject, text, user1) -> {
            sentNotifications.getAndIncrement();
            assertEquals("user", user1.getUsername(), "Username");
            assertEquals("Task Four is due tomorrow!", subject, "Subject");
            assertEquals("Task Four is due tomorrow!", text, "Body");
            pass.set(true);
        });
        this.notificationManager.setDatabaseManager(databaseManager);
        this.notificationManager.init();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(1, sentNotifications.get(), "Notifications");
        assertTrue(pass.get(), "Notification content");
    }

    @Test
    public void testTaskWithDueDateTomorrowAlreadyNotified() {
        List<Todo> todos = new LinkedList<>();
        Todo todo = new Todo(4, -1, "Four");
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.now().plusDays(1));
        todo.setLastNotification(OffsetDateTime.now());
        todos.add(todo);

        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(databaseManager.getUsers()).thenReturn(userAccounts);
        when(databaseManager.getUserList("user")).thenReturn(1);
        when(databaseManager.getTodos(1)).thenReturn(todos);
        when(databaseManager.updateTodo(1, todo)).thenReturn(true);

        AtomicInteger sentNotifications = new AtomicInteger();
        AtomicBoolean pass = new AtomicBoolean(false);

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
    public void testTaskWithDueDateInNextSevenDays() {
        List<Todo> todos = new LinkedList<>();
        Todo todo = new Todo(4, -1, "Five");
        todo.setScheduled(true);
        LocalDate due = LocalDate.now().plusDays(7);
        todo.setDueDate(due);
        todos.add(todo);

        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(databaseManager.getUsers()).thenReturn(userAccounts);
        when(databaseManager.getUserList("user")).thenReturn(1);
        when(databaseManager.getTodos(1)).thenReturn(todos);
        when(databaseManager.updateTodo(1, todo)).thenReturn(true);

        AtomicInteger sentNotifications = new AtomicInteger();
        AtomicBoolean pass = new AtomicBoolean(false);

        this.notificationManager.setEmailClient((subject, text, user1) -> {
            sentNotifications.getAndIncrement();
            assertEquals("user", user1.getUsername(), "Username");
            assertEquals("Task Five is due in next 7 days", subject, "Subject");
            assertEquals("Task Five is due on "+due.format(DateTimeFormatter.ISO_DATE)+"!", text, "Body");
            pass.set(true);
        });
        this.notificationManager.setDatabaseManager(databaseManager);
        this.notificationManager.init();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(1, sentNotifications.get(), "Notifications");
        assertTrue(pass.get(), "Notification content");
    }

    @Test
    public void testTaskWithDueDateInNextSevenDaysAlreadyNotifiedNow() {
        List<Todo> todos = new LinkedList<>();
        Todo todo = new Todo(4, -1, "Five");
        todo.setScheduled(true);
        LocalDate due = LocalDate.now().plusDays(7);
        todo.setDueDate(due);
        todo.setLastNotification(OffsetDateTime.now());
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
    public void testTaskWithDueDateInNextSevenDaysAlreadyNotifiedThreeDaysAgo() {
        List<Todo> todos = new LinkedList<>();
        Todo todo = new Todo(4, -1, "Five");
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.now().plusDays(4));
        todo.setLastNotification(OffsetDateTime.now().minusDays(3));
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
        AtomicBoolean pass = new AtomicBoolean(false);

        this.notificationManager.setEmailClient((subject, text, user1) -> sentNotifications.getAndIncrement());
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

        this.notificationManager.setEmailClient((subject, text, user1) -> sentNotifications.getAndIncrement());
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
        AtomicBoolean pass = new AtomicBoolean(false);

        this.notificationManager.setEmailClient((subject, text, user1) -> {
            sentNotifications.getAndIncrement();
            assertEquals("user", user1.getUsername(), "Username");
            assertEquals("Task Eight is due today!", subject, "Subject");
            assertEquals("Task Eight is due today!\n\nDescription.", text, "Body");
            pass.set(true);
        });
        this.notificationManager.setDatabaseManager(databaseManager);
        this.notificationManager.init();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(1, sentNotifications.get(), "Notifications");
        assertTrue(pass.get(), "Notification content");
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
    public void testTaskWithOverdueDueDate() {
        List<Todo> todos = new LinkedList<>();
        Todo todo = new Todo(3, -1, "Ten");
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.now().minus(1, ChronoUnit.DAYS));
        todos.add(todo);

        DatabaseManager databaseManager = mock(DatabaseManager.class);
        when(databaseManager.getUsers()).thenReturn(userAccounts);
        when(databaseManager.getUserList("user")).thenReturn(1);
        when(databaseManager.getTodos(1)).thenReturn(todos);
        when(databaseManager.updateTodo(1, todo)).thenReturn(true);

        AtomicInteger sentNotifications = new AtomicInteger();
        AtomicBoolean pass = new AtomicBoolean(false);

        this.notificationManager.setEmailClient((subject, text, user1) -> {
            sentNotifications.getAndIncrement();
            assertEquals("user", user1.getUsername(), "Username");
            assertEquals("Task Ten is overdue!", subject, "Subject");
            assertEquals("Task Ten is overdue!", text, "Body");
            pass.set(true);
        });
        this.notificationManager.setDatabaseManager(databaseManager);
        this.notificationManager.init();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(1, sentNotifications.get(), "Notifications");
        assertTrue(pass.get(), "Notification content");
    }

}
