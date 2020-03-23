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
