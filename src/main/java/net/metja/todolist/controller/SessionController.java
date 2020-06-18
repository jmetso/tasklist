package net.metja.todolist.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Janne Metso &copy; 2020
 * @since 2020-06-04
 */
@RestController
public class SessionController {

    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);

    @PreAuthorize("hasAnyRole('ADMIN','USER','VIEW')")
    @RequestMapping(value="/api/v1/logout", produces=MediaType.APPLICATION_JSON_VALUE, method=RequestMethod.POST)
    public ResponseEntity<Boolean> logout(HttpServletRequest request) {
        try {
            request.logout();
            return new ResponseEntity<>(true, HttpStatus.OK);
        } catch(javax.servlet.ServletException e) {
            logger.error("Unable to perform logout.", e);
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
