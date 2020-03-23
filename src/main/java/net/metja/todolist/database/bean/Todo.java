package net.metja.todolist.database.bean;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Janne Metso @copy; 2019
 * @since 2019-04-21
 */
public class Todo {

    private int id;
    private int parentId;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private ZoneOffset dueTimezone;
    private String title;
    private String description;
    private List<Todo> children;
    private boolean done = false;
    private boolean scheduled = false;
    private Repeating repeating;

    public Todo() {}

    public Todo(int id) {
        this.id = id;
        this.parentId = -1;
        this.children = new LinkedList<>();
    }

    public Todo(int id, int parentId, String title) {
        this.id = id;
        this.parentId = parentId;
        this.title = title;
        this.children = new LinkedList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalTime getDueTime() {
        return dueTime;
    }

    public void setDueTime(LocalTime dueTime) {
        this.dueTime = dueTime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Todo> getChildren() {
        return children;
    }

    public void setChildren(List<Todo> children) {
        this.children = children;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public boolean isScheduled() {
        return scheduled;
    }

    public void setScheduled(boolean scheduled) {
        this.scheduled = scheduled;
    }

    public ZoneOffset getDueTimezone() {
        return dueTimezone;
    }

    public void setDueTimezone(ZoneOffset dueTimezone) {
        this.dueTimezone = dueTimezone;
    }

    public Repeating getRepeating() {
        return repeating;
    }

    public void setRepeating(Repeating repeating) {
        this.repeating = repeating;
    }

}