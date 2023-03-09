package net.metja.todolist.controller;

import net.metja.todolist.database.DatabaseManager;
import net.metja.todolist.database.bean.Repeat;
import net.metja.todolist.database.bean.Todo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Rest controller for Task list item methods.
 *
 * @author Janne Metso @copy; 2020
 * @since 2020-03-23
 */
@RestController
public class ItemController {

    private DatabaseManager databaseManager;
    private static final Logger logger = LoggerFactory.getLogger(ItemController.class);

    @PreAuthorize("hasAnyRole('ADMIN','USER', 'VIEW')")
    @RequestMapping(value = "/api/v1/items", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public ResponseEntity<List<Todo>> getTodoItems(Principal principal) {
        int listID = this.databaseManager.getUserList(principal.getName());
        if(listID > 0) {
            List<Todo> todos = this.databaseManager.getTodos(listID);
            if(todos != null && todos.size() > 1) {
                this.sortTodosByDueDate(todos);
            }
            if(todos != null) {
                return new ResponseEntity<>(todos, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new LinkedList<>(), HttpStatus.OK);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    private void sortTodosByDueDate(List<Todo> todos) {
        todos.sort(new Comparator<Todo>() {
            @Override
            public int compare(Todo t1, Todo t2) {
                if(t1.isScheduled() || t2.isScheduled()) {
                    if (t1.getDueDate() != null && t2.getDueDate() == null) {
                        return -1;
                    } else if (t1.getDueDate() == null && t2.getDueDate() != null) {
                        return 1;
                    } else if (t1.getDueDate().isBefore(t2.getDueDate())) {
                        return -1;
                    } else if (t1.getDueDate().isAfter(t2.getDueDate())) {
                        return 1;
                    } else if(t1.getDueDate().isEqual(t2.getDueDate())) {
                        if(t1.getDueDate() == null && t2.getDueDate() == null) {
                        } else if(t1.getDueDate() != null && t2.getDueDate() == null) {
                            return -1;
                        } else if(t1.getDueDate() == null && t2.getDueDate() != null) {
                            return 1;
                        } else if(t1.getDueTime().isBefore(t2.getDueTime())) {
                            return -1;
                        } else if (t1.getDueTime().isAfter(t2.getDueTime())) {
                            return 1;
                        }
                    }
                }
                return 0;
            }
        });
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @RequestMapping(value="/api/v1/items/add", produces=MediaType.APPLICATION_JSON_VALUE, method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Integer> addTodoListItem(@RequestBody Todo todo, Principal principal) {
        int listId = this.databaseManager.getUserList(principal.getName());
        if(listId > 0) {
            int id = this.databaseManager.addTodo(listId, todo);
            if(id > 0) {
                return new ResponseEntity<>(id, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>(-1, HttpStatus.BAD_REQUEST);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @RequestMapping(value = "/api/v1/items/{id}/update", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    public ResponseEntity updateTodoListItem(@RequestBody Todo todo, @PathVariable(value="id") int id, Principal principal) {
        int listId = this.databaseManager.getUserList(principal.getName());
        if(listId > 0) {
            Todo oldTodo = this.databaseManager.getTodo(listId, id);
            if(oldTodo != null) {
                todo.setId(id);
                if(this.databaseManager.updateTodo(listId, todo)) {
                    return new ResponseEntity(HttpStatus.OK);
                } else {
                    return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else {
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @RequestMapping(value = "/api/v1/items/{id}/done", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public ResponseEntity<Boolean> markTodoListItemAsDone(@PathVariable(value="id") int id, Principal principal){
        int listId = this.databaseManager.getUserList(principal.getName());
        if(listId > 0) {
            Todo todo = this.databaseManager.getTodo(listId, id);
            if(todo != null) {
                if(!todo.isDone()) {
                    if(todo.getRepeat() == null || todo.getRepeat().getPeriod() == Repeat.TimePeriod.None) {
                        todo.setDone(true);
                        if(this.databaseManager.updateTodo(listId, todo)) {
                            return new ResponseEntity<>(true, HttpStatus.OK);
                        } else {
                            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    } else {
                        switch (todo.getRepeat().getPeriod()) {
                            case Days: todo.setDueDate(todo.getDueDate().plusDays(todo.getRepeat().getTimes())); break;
                            case Weeks: todo.setDueDate(todo.getDueDate().plusWeeks(todo.getRepeat().getTimes())); break;
                            case Months: todo.setDueDate(todo.getDueDate().plusMonths(todo.getRepeat().getTimes())); break;
                            case Years: todo.setDueDate(todo.getDueDate().plusYears(todo.getRepeat().getTimes())); break;
                        }
                        this.databaseManager.updateTodo(listId, todo);
                        return new ResponseEntity<>(true, HttpStatus.OK);
                    }
                } else {
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @RequestMapping(value = "/api/v1/items/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public ResponseEntity<Boolean> markTodoListItemAsActive(@PathVariable(value="id") int id, Principal principal){
        int listId = this.databaseManager.getUserList(principal.getName());
        if(listId > 0) {
            Todo todo = this.databaseManager.getTodo(listId, id);
            if(todo != null) {
                logger.debug("Scheduled: "+todo.isScheduled()+" Repeating: "+todo.getRepeat());
                if(todo.isDone()) {
                    todo.setDone(false);
                    if(this.databaseManager.updateTodo(listId, todo)) {
                        return new ResponseEntity<>(true, HttpStatus.OK);
                    } else {
                        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                } else {
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @RequestMapping(value = "/api/v1/items/{id}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public ResponseEntity<Boolean> markTodoListItemAsInactive(@PathVariable(value="id") int id, Principal principal){
        int listId = this.databaseManager.getUserList(principal.getName());
        if(listId > 0) {
            Todo todo = this.databaseManager.getTodo(listId, id);
            if(todo != null) {
                if(!todo.isDone() && todo.isScheduled() && todo.getRepeat() != null && todo.getRepeat().getPeriod() != Repeat.TimePeriod.None) {
                    todo.setDone(true);
                    if (this.databaseManager.updateTodo(listId, todo)) {
                        return new ResponseEntity<>(true, HttpStatus.OK);
                    } else {
                        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                } else {
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @RequestMapping(value = "/api/v1/items/{id}/delete", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public ResponseEntity<Boolean> deleteTodoListItem(@PathVariable(value="id") int id, Principal principal) {
        int listId = this.databaseManager.getUserList(principal.getName());
        if(listId > 0) {
            if(this.databaseManager.deleteTodo(listId, id)) {
                return new ResponseEntity<>(true, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(false, HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }
    }

    @Autowired
    void setDatabaseManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

}
