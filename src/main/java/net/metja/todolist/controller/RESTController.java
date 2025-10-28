package net.metja.todolist.controller;

import net.metja.todolist.database.DatabaseManager;
import net.metja.todolist.database.bean.UserAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * @author Janne Metso @copy; 2019
 * @since 2019-04-21
 */
@RestController
public class RESTController {

    private static final Logger logger = LoggerFactory.getLogger(RESTController.class);

    private DatabaseManager databaseManager;

    //@PreAuthorize("hasAnyRole('ADMIN','USER','VIEW')")
    @RequestMapping(value = "/api/v1/hello/{name}", produces= MediaType.APPLICATION_JSON_VALUE, method= RequestMethod.GET)
    public ResponseEntity<String> hello(@PathVariable(value="name", required=true)String name) {
        logger.info("Hello "+name+"!");
        return new ResponseEntity<>("{\"hello\":\""+name+"!\"}", HttpStatus.OK);
    }

    //@PreAuthorize("hasAnyRole('ADMIN','USER', 'VIEW')")
    @RequestMapping(value = "/api/v1/user", produces= MediaType.APPLICATION_JSON_VALUE, method= RequestMethod.GET)
    public ResponseEntity<String> getUser(Principal principal) {
        if(principal != null) {
            String user = "";
            logger.trace("Principal: "+principal);
            if(principal instanceof OAuth2AuthenticationToken) {
                OAuth2AuthenticationToken token = (OAuth2AuthenticationToken)principal;
                logger.trace("Token: "+token);
                logger.trace("Authorities: "+token.getAuthorities());
                user = (String)token.getPrincipal().getAttributes().get("preferred_username");
                String email = (String)token.getPrincipal().getAttributes().get("email");
                boolean userFound = false;
                List<UserAccount> users = this.databaseManager.getUsers();
                for(UserAccount userAccount: users) {
                    if(userAccount.getUsername().equalsIgnoreCase(user)) {
                        userFound = true;
                    }
                }
                if(!userFound) {
                    logger.info("Adding new user \""+user+"\" to database");
                    this.databaseManager.addUser(user, email);
                }
            } else {
                user = principal.getName();
            }
            logger.debug("Username: "+user);
            return new ResponseEntity<>("{\"user\": \"" + user + "\"}", HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value="/api/v1/password/generate/{password}",produces=MediaType.TEXT_PLAIN_VALUE,method = RequestMethod.GET)
    public ResponseEntity<String> generatePassword(@PathVariable(value="password", required = true)String password) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
        String hashedPassword = passwordEncoder.encode(password);
        return new ResponseEntity<>(hashedPassword, HttpStatus.OK);
    }

    //@PreAuthorize("hasAnyRole('ADMIN','USER','VIEW')")
    @RequestMapping(value="/api/v1/version",produces = MediaType.APPLICATION_JSON_VALUE,method = RequestMethod.GET)
    public ResponseEntity<String> version() {
        return new ResponseEntity<>("{\"version\":\""+getClass().getPackage().getImplementationVersion()+"\"}", HttpStatus.OK);
    }

    @Autowired
    public void setDatabaseManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

}
