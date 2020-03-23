package net.metja.todolist.database;

import net.metja.todolist.database.bean.Repeating;
import net.metja.todolist.database.bean.Todo;
import net.metja.todolist.database.bean.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Janne Metso @copy; 2019
 * @since 2019-04-21
 */
@Component
public class DatabaseManagerImpl implements DatabaseManager {

    static final String CREATE_SCHEMA_VERSION_TABLE = "CREATE TABLE IF NOT EXISTS Settings ( Version INTEGER )";
    static final String CREATE_PERSISTENT_LOGINS_TABLE = "CREATE TABLE IF NOT EXISTS persistent_logins (username varchar(64) not null, series varchar(64) primary key, token varchar(64) not null, last_used timestamp not null)";
    static final String CREATE_TODO_LISTS_TABLE = "CREATE TABLE IF NOT EXISTS TodoLists (ID INTEGER, UserID INTEGER, FOREIGN KEY (UserID) REFERENCES UserAccounts(ID), PRIMARY KEY(ID))";
    static final String CREATE_TODO_ITEMS_TABLE = "CREATE TABLE IF NOT EXISTS TodoItems (ID INTEGER, ListID INTEGER, ParentID INTEGER, Title TEXT, Description TEXT, Done BOOLEAN, Scheduled BOOLEAN, DueDate TEXT, DueTime TEXT, DueTimezone TEXT, Repeating TEXT, FOREIGN KEY (ListID) REFERENCES TodoList(ID), PRIMARY KEY (ID, ListID))";
    static final String CREATE_USER_ACCOUNTS_TABLE = "CREATE TABLE IF NOT EXISTS UserAccounts (ID INTEGER PRIMARY KEY, Username VARCHAR(64), Password VARCHAR(60), Roles TEXT)";
    private static final int SCHEMA_VERSION_MIN = 1;
    private static final int SCHEMA_VERSION_MAX = 1;

    private JdbcTemplate jdbcTemplate;

    private static Logger logger = LoggerFactory.getLogger(DatabaseManagerImpl.class);

    DatabaseManagerImpl() {}

    @Override
    public int addTodo(final int listID, final Todo todo) {
        int id = this.getNextTodoItemID(listID);

        final String SELECT = "SELECT ID FROM TodoLists WHERE ID=?";
        final String INSERT = "INSERT INTO TodoItems (ID, ListID, ParentID, Title, Description, Done, Scheduled, DueDate, DueTime, DueTimezone, Repeating) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            String dueTimezone = null;
            if(todo.getDueTimezone() != null) {
                dueTimezone = todo.getDueTimezone().toString();
            }
            this.jdbcTemplate.queryForObject(SELECT, Integer.class, listID);
            this.jdbcTemplate.update(INSERT, id, listID, todo.getParentId(), todo.getTitle(), todo.getDescription(),
                    todo.isDone(), todo.isScheduled(), todo.getDueDate(), todo.getDueTime(), dueTimezone,
                    todo.getRepeating());
            return id;
        } catch(org.springframework.dao.DataAccessException e) {
            logger.warn("Unable to add list item "+todo.getTitle()+" to list "+listID, e);
            return -1;
        }
    }

    @Override
    public List<Todo> getTodos(final int listID) {
        final String SELECT = "SELECT * FROM TodoItems WHERE ListID=?";
        try {
            List<Todo> todos = this.jdbcTemplate.query(SELECT, this::mapTodoItem, listID);
            for(Todo todo: todos) {
                if(todo.getParentId() > 0) {
                    for(Todo parent: todos) {
                        if(parent.getId() == todo.getParentId()) {
                            if(parent.getChildren() == null) {
                                parent.setChildren(new LinkedList<>());
                            }
                            parent.getChildren().add(todo);
                            break;
                        }
                    }
                }
            }
            return todos;
        } catch(org.springframework.dao.DataAccessException e) {
            logger.debug("Unable to fetch any todos for list "+listID);
            return null;
        }
    }

    @Override
    public boolean updateTodo(final int listId, final Todo todo) {
        final String SELECT = "SELECT * FROM TodoItems WHERE ListID=? AND ID=?";
        try {
            if(this.jdbcTemplate.queryForObject(SELECT, this::mapTodoItem, listId, todo.getId()) != null) {
                final String UPDATE = "UPDATE TodoItems SET ParentID=?, DueDate=?, DueTime=?, Title=?, Description=?, Done=?, DueTimezone=?, Scheduled=?, Repeating=? WHERE ID=? AND ListID=?";
                this.jdbcTemplate.update(UPDATE, todo.getParentId(), todo.getDueDate(), todo.getDueTime(), todo.getTitle(), todo.getDescription(), todo.isDone(), todo.getDueTimezone(), todo.isScheduled(), todo.getRepeating(), todo.getId(), listId);
                return true;
            }
        } catch(org.springframework.dao.DataAccessException e) {
            logger.debug("Updating todo "+todo.getId()+" in list "+listId+" failed.", e);
        }
        return false;
    }

    @Override
    public boolean deleteTodo(final int listId, final int todoId) {
        final String SELECT = "SELECT ID FROM TodoItems WHERE ID=? AND ListID=?";
        final String DELETE = "DELETE FROM TodoItems WHERE ListID=? AND ID=?";
        try {
            this.jdbcTemplate.queryForObject(SELECT, Integer.class, todoId, listId);
            this.jdbcTemplate.update(DELETE, listId, todoId);
            return true;
        } catch(org.springframework.dao.DataAccessException e) {
            logger.debug("Unable to delete todo "+todoId+" for list "+listId+".", e);
            return false;
        }
    }

    @Override
    public Todo getTodo(int listId, int id) {
        final String SELECT = "SELECT * FROM TodoItems WHERE ID=? AND ListID=?";
        try {
            return this.jdbcTemplate.queryForObject(SELECT, this::mapTodoItem, id, listId);
        } catch (org.springframework.dao.DataAccessException e) {
            logger.debug("Unable to fetch todo "+id+" from list "+listId+".", e);
        }
        return null;
    }

    @Override
    public int addList(final String username) {
        final String SELECT_USERID = "SELECT ID FROM UserAccounts WHERE Username=?";
        final String SELECT = "SELECT ID FROM TodoLists WHERE UserID=?";
        try {
            int userId = this.jdbcTemplate.queryForObject(SELECT_USERID, Integer.class, username);
            return this.jdbcTemplate.queryForObject(SELECT, Integer.class, userId);
        } catch(org.springframework.dao.DataAccessException e) {
            int id = this.getNextTodoListID();
            final String INSERT = "INSERT INTO TodoLists (ID, UserID) VALUES (?, ?)";
            try {
                int userId = this.jdbcTemplate.queryForObject(SELECT_USERID, Integer.class, username);
                this.jdbcTemplate.update(INSERT, id, userId);
                return id;
            } catch (org.springframework.dao.DataAccessException ex) {
                logger.error("Unable to add list for user "+username, ex);
                return -1;
            }
        }
    }

    @Override
    public int getUserList(final String username) {
        final String SELECT_USERID = "SELECT ID FROM UserAccounts WHERE Username=?";
        final String SELECT = "SELECT ID FROM TodoLists WHERE UserID=?";
        try {
            int id = this.jdbcTemplate.queryForObject(SELECT_USERID, Integer.class, username);
            return this.jdbcTemplate.queryForObject(SELECT, Integer.class, id);
        } catch(org.springframework.dao.DataAccessException e) {
            logger.debug("Unable to find todo list for user "+username);
            return -1;
        }
    }

    @Override
    public List<UserAccount> getUsers() {
        final String SELECT = "SELECT * FROM UserAccounts";
        try {
            return this.jdbcTemplate.query(SELECT, this::mapUserAccount);
        } catch(org.springframework.dao.DataAccessException e) {
            logger.debug("Unable to fetch any users.");
        }
        return null;
    }

    private synchronized int getNextTodoItemID(final int listID) {
        final String SELECT_ID = "SELECT ID FROM TodoItems WHERE ListID=? ORDER BY ID DESC LIMIT 1";
        try {
            int result = this.jdbcTemplate.queryForObject(SELECT_ID, Integer.class, listID);
            logger.debug("LastID: "+result);
            return (result+1);
        } catch(org.springframework.dao.DataAccessException e) {
            logger.warn("Could not determine last todo item id!", e);
            return 1;
        }
    }

    private synchronized int getNextTodoListID() {
        final String SELECT_ID = "SELECT ID FROM TodoLists ORDER BY ID DESC LIMIT 1";
        try {
            return (this.jdbcTemplate.queryForObject(SELECT_ID, Integer.class) +1);
        } catch(org.springframework.dao.DataAccessException e) {
            return 1;
        }
    }

    private void createTables() {
        this.jdbcTemplate.execute(CREATE_SCHEMA_VERSION_TABLE);
        this.jdbcTemplate.execute(CREATE_USER_ACCOUNTS_TABLE);
        this.jdbcTemplate.execute(CREATE_TODO_LISTS_TABLE);
        this.jdbcTemplate.execute(CREATE_TODO_ITEMS_TABLE);
        this.jdbcTemplate.execute(CREATE_PERSISTENT_LOGINS_TABLE);
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
        return todo;
    }

    private UserAccount mapUserAccount(ResultSet rs, int rowNum) throws java.sql.SQLException {
        List<String> roles = new LinkedList<>();
        StringTokenizer st = new StringTokenizer(rs.getString("Roles"), ",");
        while(st.hasMoreTokens()) {
            roles.add(st.nextToken());
        }
        return new UserAccount(rs.getInt("ID"), rs.getString("Username"), rs.getString("Password"), roles);
    }

    @PostConstruct
    public void checkDatabaseVersion() {
        final String SELECT = "SELECT Version FROM Settings";
        try {
            int version = this.jdbcTemplate.queryForObject(SELECT, Integer.class);
            if(version >= this.SCHEMA_VERSION_MIN && version <= this.SCHEMA_VERSION_MAX) {
                logger.debug("Supported database schema version.");
            } else {
                logger.error("Unsupported database schema version!");
                System.exit(1);
            }
        } catch(org.springframework.dao.DataAccessException e) {
            logger.debug("No database version set. Setting MAX supported version");
            final String UPDATE = "INSERT INTO Settings (Version) VALUES (?)";
            try {
                this.jdbcTemplate.update(UPDATE, this.SCHEMA_VERSION_MAX);
            } catch(org.springframework.dao.DataAccessException ex) {
                logger.error("Unable to set database schema version!", ex);
            }
        }
    }

    @Autowired
    void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.createTables();
    }

}
