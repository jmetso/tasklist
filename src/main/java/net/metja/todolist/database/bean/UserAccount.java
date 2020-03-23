package net.metja.todolist.database.bean;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Janne Metso @copy; 2019
 * @since 2019-11-18
 */
public class UserAccount {

    private int id;
    private String username;
    private String password;
    private List<String> roles;

    public UserAccount() {
        this.id = 1;
        this.username = "";
        this.password = "";
        this.roles = new LinkedList<>();
    }

    public UserAccount(int id, String username, String password, List<String> roles) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.roles = roles;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public List<String> getRoles() {
        return roles;
    }

}
