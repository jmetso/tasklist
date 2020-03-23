package net.metja.todolist.controller;

import net.metja.todolist.database.DatabaseManager;
import net.metja.todolist.database.bean.Repeating;
import net.metja.todolist.database.bean.Todo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;

/**
 * @author Janne Metso @copy; 2019
 * @since 2019-04-21
 */
@RestController
public class RESTController {

    private DatabaseManager databaseManager;
    private static Logger logger = LoggerFactory.getLogger(RESTController.class);

    @PreAuthorize("hasAnyRole('ADMIN','USER', 'VIEW')")
    @RequestMapping(value = "/api/v1/items", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public ResponseEntity<List<Todo>> getTodoItems(Principal principal) {
        int listID = this.databaseManager.getUserList(principal.getName());
        if(listID > 0) {
            List<Todo> todos = this.databaseManager.getTodos(listID);
            return new ResponseEntity<>(todos, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @RequestMapping(value = "/api/v1/new", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    public ResponseEntity<Boolean> addList(Principal principal) {
        int listId = this.databaseManager.addList(principal.getName());
        if(listId > 0) {
            return new ResponseEntity<>(true, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }
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
                    if(todo.getRepeating() == null || todo.getRepeating() == Repeating.No) {
                        todo.setDone(true);
                        if(this.databaseManager.updateTodo(listId, todo)) {
                            return new ResponseEntity<>(true, HttpStatus.OK);
                        } else {
                            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    } else {
                        switch (todo.getRepeating()) {
                            case Daily: todo.setDueDate(todo.getDueDate().plusDays(1)); break;
                            case Weekly: todo.setDueDate(todo.getDueDate().plusWeeks(1)); break;
                            case BiWeekly: todo.setDueDate(todo.getDueDate().plusWeeks(2)); break;
                            case Monthly: todo.setDueDate(todo.getDueDate().plusMonths(1)); break;
                            case Yearly: todo.setDueDate(todo.getDueDate().plusYears(1)); break;
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
                logger.debug("Scheduled: "+todo.isScheduled()+" Repeating: "+todo.getRepeating());
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
                if(!todo.isDone() && todo.isScheduled() && todo.getRepeating() != null && todo.getRepeating() != Repeating.No) {
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

    @PreAuthorize("hasAnyRole('ADMIN','USER', 'VIEW')")
    @RequestMapping(value = "/api/v1/hello/{name}", produces= MediaType.APPLICATION_JSON_VALUE, method= RequestMethod.GET)
    public ResponseEntity<String> hello(@PathVariable(value="name", required=true)String name) {
        logger.info("Hello "+name+"!");
        return new ResponseEntity<>("{\"hello\":\""+name+"!\"}", HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER', 'VIEW')")
    @RequestMapping(value = "/api/v1/user", produces= MediaType.APPLICATION_JSON_VALUE, method= RequestMethod.GET)
    public ResponseEntity<String> getUser(Principal principal) {
        if(principal != null) {
            return new ResponseEntity<>("{\"user\": \"" + principal.getName() + "\"}", HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER','VIEW')")
    @RequestMapping(value="/api/v1/logout",produces=MediaType.APPLICATION_JSON_VALUE,method=RequestMethod.POST)
    public ResponseEntity<Boolean> logout(HttpServletRequest request) {
        try {
            request.logout();
            return new ResponseEntity<>(true, HttpStatus.OK);
        } catch(javax.servlet.ServletException e) {
            this.logger.error("Unable to perform logout.", e);
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value="/api/v1/password/generate/{password}",produces=MediaType.TEXT_PLAIN_VALUE,method = RequestMethod.GET)
    public ResponseEntity<String> generatePassword(@PathVariable(value="password", required = true)String password) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
        String hashedPassword = passwordEncoder.encode(password);
        return new ResponseEntity<>(hashedPassword, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER','VIEW')")
    @RequestMapping(value="/api/v1/version",produces = MediaType.APPLICATION_JSON_VALUE,method = RequestMethod.GET)
    public ResponseEntity<String> version() {
        return new ResponseEntity<>("{\"version\":\""+getClass().getPackage().getImplementationVersion()+"\"}", HttpStatus.OK);
    }

    @Autowired
    void setDatabaseManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

}
