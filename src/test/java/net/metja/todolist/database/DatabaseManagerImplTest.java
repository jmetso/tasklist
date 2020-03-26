package net.metja.todolist.database;

import net.metja.todolist.database.bean.Repeating;
import net.metja.todolist.database.bean.Todo;
import net.metja.todolist.database.bean.UserAccount;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Janne Metso, @copy; 2019
 * @since 2019-04-21
 */
public class DatabaseManagerImplTest {

    private DatabaseManagerImpl impl;
    private JdbcTemplate jdbcTemplate;

    @Before
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

    @After
    public void tearDown() {
        this.impl = null;
    }

    @Test
    public void addTodo_Minimal() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'Test')");
        this.jdbcTemplate.execute("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        Todo todo = new Todo(1, -1, "Title");

        int id = this.impl.addTodo(1, todo);
        assertEquals("ID", 1, id);

        Todo result = this.jdbcTemplate.queryForObject("SELECT * FROM TodoItems WHERE ID=1", this::mapTodoItem);
        assertNotNull("Result", result);
        assertEquals("ID", 1, result.getId());
        assertEquals("Title", "Title", result.getTitle());
        assertFalse("Done", todo.isDone());
        assertNull("Description", todo.getDescription());
        assertFalse("Scheduled", todo.isScheduled());
        assertNull("DueDate", todo.getDueDate());
        assertNull("DueTime", todo.getDueTime());
        assertNull("DueTimezone", todo.getDueTimezone());
        assertNull("Repeating", todo.getRepeating());
        assertNull("LastNotification", todo.getLastNotification());
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
        todo.setRepeating(Repeating.Weekly);
        OffsetDateTime lastNotification = OffsetDateTime.now();
        todo.setLastNotification(lastNotification);

        int id = this.impl.addTodo(1, todo);
        assertEquals("ID", 2, id);

        Todo result = this.jdbcTemplate.queryForObject("SELECT * FROM TodoItems WHERE ID=2", this::mapTodoItem);
        assertNotNull("Result", result);
        assertEquals("ID", 2, result.getId());
        assertEquals("Title", "Title", result.getTitle());
        assertEquals("Description", "Description", result.getDescription());
        assertTrue("Done", result.isDone());
        assertTrue("Scheduled", result.isScheduled());
        assertEquals("DueDate", LocalDate.of(2019, 4, 21), result.getDueDate());
        assertEquals("DueTime", LocalTime.of(12, 0, 0), result.getDueTime());
        assertEquals("DueTimezone", ZoneOffset.of("+02:30"), result.getDueTimezone());
        assertEquals("Repeating", Repeating.Weekly, result.getRepeating());
        assertNotNull("LastNotification", result.getLastNotification());
        assertEquals("LastNotification", lastNotification.toString(), result.getLastNotification().toString());
    }

    @Test
    public void readTodo_AllData() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'Test')");
        this.jdbcTemplate.execute("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title, Description, Done, Scheduled, DueDate, DueTime, DueTimezone, Repeating, LastNotification) VALUES (1, 1, -1, \"Parent\", \"Description\", 0, 1, '2019-11-23', '14:56', '+02:00', 'No', '2020-03-26T16:26:00+02:00')");

        Todo todo = this.impl.getTodo(1, 1);
        assertNotNull("Todo", todo);
        assertEquals("ID", 1, todo.getId());
        assertEquals("ParentID", -1, todo.getParentId());
        assertEquals("Title", "Parent", todo.getTitle());
        assertEquals("Description", "Description", todo.getDescription());
        assertFalse("Done", todo.isDone());
        assertTrue("Scheduled", todo.isScheduled());
        assertEquals("DueDate", LocalDate.of(2019, 11, 23), todo.getDueDate());
        assertEquals("DueTime", LocalTime.of(14, 56, 0), todo.getDueTime());
        assertEquals("DueTimezone", ZoneOffset.of("+02:00"), todo.getDueTimezone());
        assertEquals("Repeating", Repeating.No, todo.getRepeating());
        assertNotNull("LastNotification", todo.getLastNotification());
        assertEquals("LastNotification", "2020-03-26T16:26+02:00", todo.getLastNotification().toString());
    }
    @Test
    public void addTodo_IDClash() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'Test')");
        this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID) VALUES (1, 1)");

        Todo todo = new Todo(1, -1, "Title");
        int id = this.impl.addTodo(1, todo);
        assertEquals("ID", 2, id);
        Todo result = this.jdbcTemplate.queryForObject("SELECT * FROM TodoItems where ID=2", this::mapTodoItem);
        assertNotNull("Result", result);
        assertEquals("ID", 2, result.getId());
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
        assertEquals("ID", 1, id);

        Todo result = this.jdbcTemplate.queryForObject("SELECT * FROM TodoItems WHERE ID=1 AND ListID=2", this::mapTodoItem);
        assertNotNull("Result", result);
        assertEquals("ID", 1, result.getId());
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
        assertNotNull("Todos", todos);
        assertEquals("Size", 2, todos.size());
        assertEquals("1 ID", 1, todos.get(0).getId());
        assertEquals("1 Parent", -1, todos.get(0).getParentId());
        assertEquals("1 Title", "Parent", todos.get(0).getTitle());
        assertFalse("Done", todos.get(0).isDone());
        assertNotNull("1 Children", todos.get(0).getChildren());
        assertEquals("1 Children size", 1, todos.get(0).getChildren().size());
        assertEquals("1 Children ID", 2, todos.get(0).getChildren().get(0).getId());
        assertEquals("1 Children Parent", 1, todos.get(0).getChildren().get(0).getParentId());
        assertEquals("1 Children Title", "Child", todos.get(0).getChildren().get(0).getTitle());

        assertEquals("2 ID", 2, todos.get(1).getId());
        assertEquals("2 Parent", 1, todos.get(1).getParentId());
        assertEquals("2 Title", "Child", todos.get(1).getTitle());
        assertFalse("Done", todos.get(1).isDone());
    }

    @Test
    public void getTodos_UnknownID() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'UserOne')");
        this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (2, 1, 1, \"Child\")");

        List<Todo> todos = this.impl.getTodos(2);
        assertNotNull("Todos", todos);
        assertEquals("Size", 0, todos.size());
    }

    @Test
    public void updateTodo() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'UserOne')");
        this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (2, 1, 1, \"Child\")");

        Todo todo = new Todo(2, -1, "Task2");
        boolean result = this.impl.updateTodo(1, todo);

        assertTrue("Result", result);
        Todo todoUpd = this.jdbcTemplate.queryForObject("SELECT * FROM TodoItems WHERE ListID=1 and ID=2", this::mapTodoItem);
        assertNotNull("Todo", todoUpd);
        assertEquals("ParentID", -1, todoUpd.getParentId());
        assertEquals("Title", "Task2", todoUpd.getTitle());
    }

    @Test(expected = org.springframework.dao.EmptyResultDataAccessException.class)
    public void updateTodo_NonExisting() throws Exception {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'UserOne')");
        this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");

        Todo todo = new Todo(2, -1, "Task2");
        boolean result = this.impl.updateTodo(1, todo);

        assertFalse("Result", result);
        Todo todoUpd = this.jdbcTemplate.queryForObject("SELECT * FROM TodoItems WHERE ListID=1 and ID=2", this::mapTodoItem);
    }

    @Test
    public void addList() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'User')");
        int id = this.impl.addList("User");
        assertEquals("ID", 1, id);
        assertEquals("ID", 1, (int)this.jdbcTemplate.queryForObject("SELECT ID FROM TodoLists WHERE UserID=?", Integer.class, 1));
        assertEquals("UserID", 1, (int)this.jdbcTemplate.queryForObject("SELECT UserID FROM TodoLists WHERE ID=?", Integer.class, 1));
    }

    @Test
    public void addList_ExistingList() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'Test')");
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (2,'User')");
        this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        int id = this.impl.addList("User");
        assertEquals("ID", 2, id);
        assertEquals("ID", 2, (int)this.jdbcTemplate.queryForObject("SELECT ID FROM TodoLists WHERE UserID=?", Integer.class, 2));
        assertEquals("User", 2, (int)this.jdbcTemplate.queryForObject("SELECT UserID FROM TodoLists WHERE ID=?", Integer.class, 2));
    }

    @Test
    public void addList_UsernameClash() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'User')");
        this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        int id = this.impl.addList("User");
        assertEquals("ID", 1, id);
    }

    @Test
    public void getUserList() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'UserOne')");
        this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");

        int id = this.impl.getUserList("UserOne");
        assertEquals("ID", 1, id);
    }

    @Test
    public void getUserList_UnknownUser() {
        int id = this.impl.getUserList("UserOne");
        assertEquals("ID", -1, id);
    }

    @Test(expected = org.springframework.dao.EmptyResultDataAccessException.class)
    public void deleteTodo() throws Exception {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'Test')");
        this.jdbcTemplate.execute("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");

        boolean result = this.impl.deleteTodo(1, 1);
        assertTrue("Result", result);
        this.jdbcTemplate.queryForObject("SELECT ID FROM TodoItems WHERE ID=? AND ListID=?", Integer.class, 1, 1);
    }

    @Test
    public void deleteTodo_UnknownID() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'Test')");
        this.jdbcTemplate.execute("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");

        boolean result = this.impl.deleteTodo(1, 2);
        assertFalse("Result", result);
    }

    @Test
    public void deleteTodo_UnknownList() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'Test')");
        this.jdbcTemplate.execute("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");

        boolean result = this.impl.deleteTodo(2, 2);
        assertFalse("Result", result);
    }

    @Test
    public void getTodo() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID,Username) VALUES (1,'Test')");
        this.jdbcTemplate.execute("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");

        Todo todo = this.impl.getTodo(1, 1);
        assertNotNull("Todo", todo);
        assertEquals("ID", 1, todo.getId());
        assertEquals("ParentID", -1, todo.getParentId());
        assertEquals("Title", "Parent", todo.getTitle());
        assertNull("DueDate", todo.getDueDate());
        assertNull("DueTime", todo.getDueTime());
        assertNull("Description", todo.getDescription());
        assertFalse("Done", todo.isDone());
        assertNotNull("Children", todo.getChildren());
        assertEquals("Size", 0, todo.getChildren().size());
    }

    @Test
    public void getTodo_UnknownList() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID, Username) VALUES (1, 'Test')");
        this.jdbcTemplate.update("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");

        Todo todo = this.impl.getTodo(2, 1);
        assertNull("Todo", todo);
    }

    @Test
    public void getTodo_UnknownItem() {
        this.jdbcTemplate.update("INSERT INTO UserAccounts (ID, Username) VALUES (1, 'Test')");
        this.jdbcTemplate.execute("INSERT INTO TodoLists (ID, UserID) VALUES (1, 1)");
        this.jdbcTemplate.update("INSERT INTO TodoItems (ID, ListID, ParentID, Title) VALUES (1, 1, -1, \"Parent\")");

        Todo todo = this.impl.getTodo(1, 2);
        assertNull("Todo", todo);
    }

    @Test
    public void getUsers() {
        this.jdbcTemplate.execute("INSERT INTO UserAccounts (ID, Username, Password, Roles, Email) VALUES (1, 'user', 'user', 'ADMIN,USER', 'test@example.com')");

        List<UserAccount> users = this.impl.getUsers();
        assertNotNull("Users", users);
        assertEquals("Size", 1, users.size());
        assertEquals("ID", 1, users.get(0).getId());
        assertEquals("Username", "user", users.get(0).getUsername());
        assertEquals("Password", "user", users.get(0).getPassword());
        assertNotNull("Roles", users.get(0).getRoles());
        assertEquals("Roles size", 2, users.get(0).getRoles().size());
        assertEquals("Role one", "ADMIN", users.get(0).getRoles().get(0));
        assertEquals("Role two", "USER", users.get(0).getRoles().get(1));
        assertEquals("Email", "test@example.com", users.get(0).getEmail());
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
            todo.setRepeating(Repeating.getRepeating(rs.getString("Repeating")));
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

        assertEquals("Version number", 1, (int)this.jdbcTemplate.queryForObject(SELECT_VERSION, Integer.class));
        this.impl = new DatabaseManagerImpl();
        this.impl.setDataSource(dataSource);
        this.impl.migrateDatabaseToLatestVersion();

        final String INSERT_USER = "INSERT INTO UserAccounts (ID, Username, Password, Roles, Email) VALUES (?, ?, ?, ?, ?)";
        final String INSERT_TODO_ITEM = "INSERT INTO TodoItems (ID, ListID, ParentID, Title, Description, Done, Scheduled, LastNotification) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        final String SELECT_LAST_NOTIFICATION = "SELECT LastNotification FROM TodoItems WHERE ID=?";
        final String SELECT_EMAIL = "SELECT Email FROM UserAccounts WHERE ID=?";

        assertEquals("Version number", 2, (int)this.jdbcTemplate.queryForObject(SELECT_VERSION, Integer.class));
        assertEquals("User insert", 1, this.jdbcTemplate.update(INSERT_USER, 1, "user", "pwd", "ADMIN, USER", "test@example.com"));
        assertEquals("Todo item insert", 1, this.jdbcTemplate.update(INSERT_TODO_ITEM, 1, 1, -1, "Title", "Description", 0, 0, "2020-03-26T16:10:00+0200"));
        assertEquals("Email", "test@example.com", this.jdbcTemplate.queryForObject(SELECT_EMAIL, String.class, 1));
        assertEquals("Last notification", "2020-03-26T16:10:00+0200", this.jdbcTemplate.queryForObject(SELECT_LAST_NOTIFICATION, String.class, 1));
    }

    @Test
    public void checkDatabaseVersion() {
        final String SELECT_VERSION = "SELECT Version FROM Settings";
        this.impl.checkDatabaseVersion();
        assertEquals("Version", 2, (int)this.jdbcTemplate.queryForObject(SELECT_VERSION, Integer.class));
    }

}
