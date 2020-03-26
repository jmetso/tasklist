package net.metja.todolist.database;

import net.metja.todolist.database.bean.Todo;
import net.metja.todolist.database.bean.UserAccount;

import java.util.List;

/**
 * @author Janne Metso @copy; 2019
 * @since 2019-04-21
 */
public interface DatabaseManager {

    int addList(String username);
    int getUserList(String username);

    int addTodo(int listId, Todo todo);
    List<Todo> getTodos(int listID);
    Todo getTodo(int listId, int id);
    boolean updateTodo(int listId, Todo todo);
    boolean deleteTodo(int listId, int todoId);

    List<UserAccount> getUsers();

    boolean migrateDatabaseToLatestVersion();
}
