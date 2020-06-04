package net.metja.todolist.controller;

import net.metja.todolist.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.security.Principal;

/**
 * @author Janne Metso &copy; 2020
 * @since 2020-06-04
 */
public class ListController {

    private DatabaseManager databaseManager;
    private static Logger logger = LoggerFactory.getLogger(ListController.class);

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

    @Autowired
    void setDatabaseManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

}
