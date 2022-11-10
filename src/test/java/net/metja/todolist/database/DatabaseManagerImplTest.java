package net.metja.todolist.database;

import net.metja.todolist.database.bean.Repeat;
import net.metja.todolist.database.bean.Todo;
import net.metja.todolist.database.bean.UserAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Janne Metso, @copy; 2019
 * @since 2019-04-21
 */
public class DatabaseManagerImplTest {

    private DatabaseManagerImpl impl;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:file::memory:");

        this.impl = new DatabaseManagerImpl();
        this.impl.setDataSource(dataSource);

        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcTemplate.execute(DatabaseManagerImpl.CREATE_SCHEMA_VERSION_TABLE);
        this.jdbcTemplate.execute(DatabaseManagerImpl.CREATE_USER_ACCOUNTS_TABLE);
        this.jdbcTemplate.execute(DatabaseManagerImpl.CREATE_PERSISTENT_LOGINS_TABLE);
        this.jdbcTemplate.execute(DatabaseManagerImpl.CREATE_TODO_ITEMS_TABLE);
        this.jdbcTemplate.execute(DatabaseManagerImpl.CREATE_TODO_LISTS_TABLE);
    }

    @AfterEach
    public void tearDown() {
        this.impl = null;
    }

    @Test
    public void addTodo_Minimal() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'Test')");
        this.jdbcTemplate.execute("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        Todo todo = new Todo(1, -1, "Title");

        int id = this.impl.addTodo(1, todo);
        assertEquals(1, id, "ID");

        Todo result = this.jdbcTemplate.queryForObject("SELECT * FROM TodoItems WHERE ID=1", this::mapTodoItem);
        assertNotNull(result, "Result");
        assertEquals(1, result.getId(), "ID");
        assertEquals("Title", "Title", result.getTitle());
        assertFalse(todo.isDone(), "Done");
        assertNull(todo.getDescription(), "Description");
        assertFalse(todo.isScheduled(), "Scheduled");
        assertNull(todo.getDueDate(), "DueDate");
        assertNull(todo.getDueTime(), "DueTime");
        assertNull(todo.getDueTimezone(), "DueTimezone");
        assertNull(todo.getRepeat(), "Repeating");
        assertNull(todo.getLastNotification(), "LastNotification");
    }

    @Test
    public void addTodo_AllData() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'Test')");
        this.jdbcTemplate.execute("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");
        Todo todo = new Todo(2);
        todo.setParentId(1);
        todo.setTitle("Title");
        todo.setDescription("Description");
        todo.setDone(true);
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.of(2019, 4, 21));
        todo.setDueTime(LocalTime.of(12, 0, 0));
        todo.setDueTimezone(ZoneOffset.of("+02:30"));
        todo.setRepeat(new Repeat(1, Repeat.TimePeriod.Weeks));
        OffsetDateTime lastNotification = OffsetDateTime.now();
        todo.setLastNotification(lastNotification);

        int id = this.impl.addTodo(1, todo);
        assertEquals(2, id, "ID");

        Todo result = this.jdbcTemplate.queryForObject("SELECT * FROM TodoItems WHERE ID=2", this::mapTodoItem);
        assertNotNull(result, "Result");
        assertEquals(2, result.getId(), "ID");
        assertEquals("Title", "Title", result.getTitle());
        assertEquals("Description", "Description", result.getDescription());
        assertTrue(result.isDone(), "Done");
        assertTrue(result.isScheduled(), "Scheduled");
        assertEquals(LocalDate.of(2019, 4, 21), result.getDueDate(), "DueDate");
        assertEquals(LocalTime.of(12, 0, 0), result.getDueTime(), "DueTime");
        assertEquals(ZoneOffset.of("+02:30"), result.getDueTimezone(), "DueTimezone");
        assertEquals(Repeat.TimePeriod.Weeks, result.getRepeat().getPeriod(), "Repeating");
        assertEquals(1, result.getRepeat().getTimes(), "Repeating");
        assertNotNull(result.getLastNotification(), "LastNotification");
        assertEquals(lastNotification.toString(), result.getLastNotification().toString(), "LastNotification");
    }

    @Test
    public void readTodo_AllData() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'Test')");
        this.jdbcTemplate.execute("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title, Description, Done, Scheduled, DueDate, DueTime, DueTimezone, Repeating, LastNotification) VALUES (1, 1, -1, \"Parent\", \"Description\", 0, 1, '2019-11-23', '14:56', '+02:00', 'No', '2020-03-26T16:26:00+02:00')");

        Todo todo = this.impl.getTodo(1, 1);
        assertNotNull(todo, "Todo");
        assertEquals(1, todo.getId(), "ID");
        assertEquals(-1, todo.getParentId(), "ParentID");
        assertEquals("Parent", todo.getTitle(), "Title");
        assertEquals("Description", todo.getDescription(), "Description");
        assertFalse(todo.isDone(), "Done");
        assertTrue(todo.isScheduled(), "Scheduled");
        assertEquals(LocalDate.of(2019, 11, 23), todo.getDueDate(), "DueDate");
        assertEquals(LocalTime.of(14, 56, 0), todo.getDueTime(), "DueTime");
        assertEquals(ZoneOffset.of("+02:00"), todo.getDueTimezone(), "DueTimezone");
        assertEquals(Repeat.TimePeriod.None, todo.getRepeat().getPeriod(), "Repeating");
        assertNotNull(todo.getLastNotification(), "LastNotification");
        assertEquals("2020-03-26T16:26+02:00", todo.getLastNotification().toString(), "LastNotification");
    }


    @Test
    public void readTodo_AllData_MonthlyRepeat() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'Test')");
        this.jdbcTemplate.execute("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title, Description, Done, Scheduled, DueDate, DueTime, DueTimezone, Repeating, LastNotification) VALUES (1, 1, -1, \"Parent\", \"Description\", 0, 1, '2019-11-23', '14:56', '+02:00', 'Every 1 Months', '2020-03-26T16:26:00+02:00')");

        Todo todo = this.impl.getTodo(1, 1);
        assertNotNull(todo, "Todo");
        assertEquals(1, todo.getId(), "ID");
        assertEquals(-1, todo.getParentId(), "ParentID");
        assertEquals("Parent", todo.getTitle(), "Title");
        assertEquals("Description", todo.getDescription(), "Description");
        assertFalse(todo.isDone(), "Done");
        assertTrue(todo.isScheduled(), "Scheduled");
        assertEquals(LocalDate.of(2019, 11, 23), todo.getDueDate(), "DueDate");
        assertEquals(LocalTime.of(14, 56, 0), todo.getDueTime(), "DueTime");
        assertEquals(ZoneOffset.of("+02:00"), todo.getDueTimezone(), "DueTimezone");
        assertEquals(Repeat.TimePeriod.Months, todo.getRepeat().getPeriod(), "Repeating");
        assertEquals(1, todo.getRepeat().getTimes(), "Repeating");
        assertNotNull(todo.getLastNotification(), "LastNotification");
        assertEquals("2020-03-26T16:26+02:00", todo.getLastNotification().toString(), "LastNotification");
    }

    @Test
    public void addTodo_IDClash() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'Test')");
        this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID) VALUES (1, 1)");

        Todo todo = new Todo(1, -1, "Title");
        int id = this.impl.addTodo(1, todo);
        assertEquals(2, id, "ID");
        Todo result = this.jdbcTemplate.queryForObject("SELECT * FROM TodoItems where ID=2", this::mapTodoItem);
        assertNotNull(result, "Result");
        assertEquals(2, result.getId(), "ID");
    }

    @Test
    public void addTodo_AnotherListWithIDsPresent() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'UserOne')");
        this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Title\")");
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (2,'UserTwo')");
        this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (2, 2)");

        Todo todo = new Todo(1, -1, "Title");
        int id = this.impl.addTodo(2, todo);
        assertEquals(1, id, "ID");

        Todo result = this.jdbcTemplate.queryForObject("SELECT * FROM TodoItems WHERE ID=1 AND ListID=2", this::mapTodoItem);
        assertNotNull(result, "Result");
        assertEquals(1, result.getId(), "ID");
        assertEquals("Title", "Title", result.getTitle());
    }

    @Test
    public void getTodos() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'UserOne')");
        this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (2, 1, 1, \"Child\")");
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (2,'UserTwo')");
        this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (2, 2)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 2, -1, \"Title\")");

        List<Todo> todos = this.impl.getTodos(1);
        assertNotNull(todos, "Todos");
        assertEquals(2, todos.size(), "Size");
        assertEquals(1, todos.get(0).getId(), "1 ID");
        assertEquals(-1, todos.get(0).getParentId(), "1 Parent");
        assertEquals("Parent", todos.get(0).getTitle(), "1 Title");
        assertFalse(todos.get(0).isDone(), "Done");
        assertNotNull(todos.get(0).getChildren(), "1 Children");
        assertEquals(1, todos.get(0).getChildren().size(), "1 Children size");
        assertEquals(2, todos.get(0).getChildren().get(0).getId(), "1 Children ID");
        assertEquals(1, todos.get(0).getChildren().get(0).getParentId(), "1 Children Parent");
        assertEquals("Child", todos.get(0).getChildren().get(0).getTitle(), "1 Children Title");

        assertEquals(2, todos.get(1).getId(), "2 ID");
        assertEquals(1, todos.get(1).getParentId(), "2 Parent");
        assertEquals("Child", todos.get(1).getTitle(), "2 Title");
        assertFalse(todos.get(1).isDone(), "Done");
    }

    @Test
    public void getTodos_UnknownID() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'UserOne')");
        this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (2, 1, 1, \"Child\")");

        List<Todo> todos = this.impl.getTodos(2);
        assertNotNull(todos, "Todos");
        assertEquals(0, todos.size(), "Size");
    }

    @Test
    public void updateTodo() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'UserOne')");
        this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (2, 1, 1, \"Child\")");

        Todo todo = new Todo(2, -1, "Task2");
        boolean result = this.impl.updateTodo(1, todo);

        assertTrue(result, "Result");
        Todo todoUpd = this.jdbcTemplate.queryForObject("SELECT * FROM TodoItems WHERE ListID=1 and ID=2", this::mapTodoItem);
        assertNotNull(todoUpd, "Todo");
        assertEquals(-1, todoUpd.getParentId(), "ParentID");
        assertEquals("Task2", todoUpd.getTitle(), "Title");
    }

    @Test
    public void updateTodoWithLastNotified() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'UserOne')");
        this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (2, 1, 1, \"Child\")");

        OffsetDateTime now = OffsetDateTime.now();
        Todo todo = new Todo(2, -1, "Task2");
        todo.setLastNotification(now);
        boolean result = this.impl.updateTodo(1, todo);

        assertTrue(result, "Result");
        Todo todoUpd = this.jdbcTemplate.queryForObject("SELECT * FROM TodoItems WHERE ListID=1 and ID=2", this::mapTodoItem);
        assertNotNull(todoUpd, "Todo");
        assertEquals(-1, todoUpd.getParentId(), "ParentID");
        assertEquals("Task2", todoUpd.getTitle(), "Title");
        assertEquals(now, todoUpd.getLastNotification(), "Last updated");
    }

    @Test
    public void updateTodo_NonExisting() {
        EmptyResultDataAccessException thrown = Assertions.assertThrows(EmptyResultDataAccessException.class, () -> {
            this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'UserOne')");
            this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
            this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");

            Todo todo = new Todo(2, -1, "Task2");
            boolean result = this.impl.updateTodo(1, todo);

            assertFalse(result, "Result");
            Todo todoUpd = this.jdbcTemplate.queryForObject("SELECT * FROM TodoItems WHERE ListID=1 and ID=2", this::mapTodoItem);
        }, "EmptyResultDataAccessException expected.");
    }

    @Test
    public void addList() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'User')");
        int id = this.impl.addList("User");
        assertEquals(1, id, "ID");
        assertEquals(1, (int)this.jdbcTemplate.queryForObject("SELECT ID FROM TodoLists WHERE UserID=?", Integer.class, 1), "ID");
        assertEquals(1, (int)this.jdbcTemplate.queryForObject("SELECT UserID FROM TodoLists WHERE ID=?", Integer.class, 1), "UserID");
    }

    @Test
    public void addList_ExistingList() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'Test')");
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (2,'User')");
        this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        int id = this.impl.addList("User");
        assertEquals(2, id, "ID");
        assertEquals(2, (int)this.jdbcTemplate.queryForObject("SELECT ID FROM TodoLists WHERE UserID=?", Integer.class, 2), "ID");
        assertEquals(2, (int)this.jdbcTemplate.queryForObject("SELECT UserID FROM TodoLists WHERE ID=?", Integer.class, 2), "User");
    }

    @Test
    public void addList_UsernameClash() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'User')");
        this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        int id = this.impl.addList("User");
        assertEquals(1, id, "ID");
    }

    @Test
    public void getUserList() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'UserOne')");
        this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");

        int id = this.impl.getUserList("UserOne");
        assertEquals(1, id, "ID");
    }

    @Test
    public void getUserList_UnknownUser() {
        int id = this.impl.getUserList("UserOne");
        assertEquals(-1, id, "ID");
    }

    @Test
    public void deleteTodo() {
        EmptyResultDataAccessException thrown = Assertions.assertThrows(EmptyResultDataAccessException.class, () -> {
            this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'Test')");
            this.jdbcTemplate.execute("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
            this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");

            boolean result = this.impl.deleteTodo(1, 1);
            assertTrue(result, "Result");
            this.jdbcTemplate.queryForObject("SELECT ID FROM TodoItems WHERE ID=? AND ListID=?", Integer.class, 1, 1);
        }, "EmptyResultDataAccessException expected");
    }

    @Test
    public void deleteTodo_UnknownID() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'Test')");
        this.jdbcTemplate.execute("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");

        boolean result = this.impl.deleteTodo(1, 2);
        assertFalse(result, "Result");
    }

    @Test
    public void deleteTodo_UnknownList() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'Test')");
        this.jdbcTemplate.execute("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");

        boolean result = this.impl.deleteTodo(2, 2);
        assertFalse(result, "Result");
    }

    @Test
    public void getTodo() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'Test')");
        this.jdbcTemplate.execute("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");

        Todo todo = this.impl.getTodo(1, 1);
        assertNotNull(todo, "Todo");
        assertEquals(1, todo.getId(), "ID");
        assertEquals(-1, todo.getParentId(), "ParentID");
        assertEquals("Parent", todo.getTitle(), "Title");
        assertNull(todo.getDueDate(), "DueDate");
        assertNull(todo.getDueTime(), "DueTime");
        assertNull(todo.getDescription(), "Description");
        assertFalse(todo.isDone(), "Done");
        assertNotNull(todo.getChildren(), "Children");
        assertEquals(0, todo.getChildren().size(), "Size");
    }

    @Test
    public void getTodo_UnknownList() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID, Username) VALUES (1, 'Test')");
        this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");

        Todo todo = this.impl.getTodo(2, 1);
        assertNull(todo, "Todo");
    }

    @Test
    public void getTodo_UnknownItem() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID, Username) VALUES (1, 'Test')");
        this.jdbcTemplate.execute("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");

        Todo todo = this.impl.getTodo(1, 2);
        assertNull(todo, "Todo");
    }

    @Test
    public void getUsers() {
        this.jdbcTemplate.execute("INSERT INTO UserAccounts (ID, Username, Password, Roles, Email) VALUES (1, 'user', 'user', 'admin,user', 'test@example.com')");

        List<UserAccount> users = this.impl.getUsers();
        assertNotNull(users, "Users");
        assertEquals(1, users.size(), "Size");
        assertEquals(1, users.get(0).getId(), "ID");
        assertEquals("user", users.get(0).getUsername(), "Username");
        assertEquals("user", users.get(0).getPassword(), "Password");
        assertNotNull(users.get(0).getRoles(), "Roles");
        assertEquals(2, users.get(0).getRoles().size(), "Roles size");
        assertEquals("ADMIN", users.get(0).getRoles().get(0), "Role one");
        assertEquals("USER", users.get(0).getRoles().get(1), "Role two");
        assertEquals("test@example.com", users.get(0).getEmail(), "Email");
    }

    private Todo mapTodoItem(ResultSet rs, int rowNum) throws java.sql.SQLException {
        Todo todo = new Todo(rs.getInt("ID"));
        todo.setParentId(rs.getInt("ParentID"));
        todo.setTitle(rs.getString("Title"));
        if(rs.getString("Description") != null) {
            todo.setDescription(rs.getString("Description"));
        }
        todo.setDone(rs.getBoolean("Done"));
        todo.setScheduled(rs.getBoolean("Scheduled"));
        if(rs.getString("DueDate") != null) {
            todo.setDueDate(LocalDate.parse(rs.getString("DueDate")));
        }
        if(rs.getString("DueTime") != null) {
            todo.setDueTime(LocalTime.parse(rs.getString("DueTime")));
        }
        if(rs.getString("DueTimezone") != null) {
            todo.setDueTimezone(ZoneOffset.of(rs.getString("DueTimezone")));
        }
        if(rs.getString("Repeating") != null) {
            todo.setRepeat(Repeat.parse(rs.getString("Repeating")));
        }
        if(rs.getString("LastNotification") != null) {
            todo.setLastNotification(OffsetDateTime.parse(rs.getString("LastNotification")));
        }
        return todo;
    }

    @Test
    public void migrateV1toLatest() {
        final String CREATE_SCHEMA_VERSION_TABLE_V1 = "CREATE TABLE IF NOT EXISTS Settings ( Version INTEGER )";
        final String CREATE_PERSISTENT_LOGINS_TABLE_V1 = "CREATE TABLE IF NOT EXISTS persistent_logins (username varchar(64) not null, series varchar(64) primary key, token varchar(64) not null, last_used timestamp not null)";
        final String CREATE_TODO_LISTS_TABLE_V1 = "CREATE TABLE IF NOT EXISTS TodoLists (ID INTEGER, UserID INTEGER, FOREIGN KEY (UserID) REFERENCES UserAccounts(ID), PRIMARY KEY(ID))";
        final String CREATE_TODO_ITEMS_TABLE_V1 = "CREATE TABLE IF NOT EXISTS TodoItems (ID INTEGER, ListID INTEGER, ParentID INTEGER, Title TEXT, Description TEXT, Done BOOLEAN, Scheduled BOOLEAN, DueDate TEXT, DueTime TEXT, DueTimezone TEXT, Repeating TEXT, FOREIGN KEY (ListID) REFERENCES TodoList(ID), PRIMARY KEY (ID, ListID))";
        final String CREATE_USER_ACCOUNTS_TABLE_V1 = "CREATE TABLE IF NOT EXISTS UserAccounts (ID INTEGER PRIMARY KEY, Username VARCHAR(64), Password VARCHAR(60), Roles TEXT)";
        final String INSERT_VERSION = "INSERT INTO Settings (Version) VALUES (?)";
        final String SELECT_VERSION = "SELECT Version FROM Settings";

        SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:file::memory:");

        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcTemplate.execute(CREATE_SCHEMA_VERSION_TABLE_V1);
        this.jdbcTemplate.execute(CREATE_USER_ACCOUNTS_TABLE_V1);
        this.jdbcTemplate.execute(CREATE_PERSISTENT_LOGINS_TABLE_V1);
        this.jdbcTemplate.execute(CREATE_TODO_ITEMS_TABLE_V1);
        this.jdbcTemplate.execute(CREATE_TODO_LISTS_TABLE_V1);
        this.jdbcTemplate.update(INSERT_VERSION, 1);

        assertEquals(1, (int)this.jdbcTemplate.queryForObject(SELECT_VERSION, Integer.class), "Version number");
        this.impl = new DatabaseManagerImpl();
        this.impl.setDataSource(dataSource);
        this.impl.migrateDatabaseToLatestVersion();

        final String INSERT_USER = "INSERT INTO UserAccounts (ID, Username, Password, Roles, Email) VALUES (?, ?, ?, ?, ?)";
        final String INSERT_TODO_ITEM = "INSERT INTO TodoItems (ID, ListID, ParentID, Title, Description, Done, Scheduled, LastNotification) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        final String SELECT_LAST_NOTIFICATION = "SELECT LastNotification FROM TodoItems WHERE ID=?";
        final String SELECT_EMAIL = "SELECT Email FROM UserAccounts WHERE ID=?";

        assertEquals(2, (int)this.jdbcTemplate.queryForObject(SELECT_VERSION, Integer.class), "Version number");
        assertEquals(1, this.jdbcTemplate.update(INSERT_USER, 1, "user", "pwd", "ADMIN, USER", "test@example.com"), "User insert");
        assertEquals(1, this.jdbcTemplate.update(INSERT_TODO_ITEM, 1, 1, -1, "Title", "Description", 0, 0, "2020-03-26T16:10:00+0200"), "Todo item insert");
        assertEquals("test@example.com", this.jdbcTemplate.queryForObject(SELECT_EMAIL, String.class, 1), "Email");
        assertEquals("2020-03-26T16:10:00+0200", this.jdbcTemplate.queryForObject(SELECT_LAST_NOTIFICATION, String.class, 1), "Last notification");
    }

    @Test
    public void checkDatabaseVersion() {
        final String SELECT_VERSION = "SELECT Version FROM Settings";
        this.impl.checkDatabaseVersion();
        assertEquals(2, (int)this.jdbcTemplate.queryForObject(SELECT_VERSION, Integer.class), "Version");
    }

}
