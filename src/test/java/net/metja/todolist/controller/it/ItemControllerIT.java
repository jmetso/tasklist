package net.metja.todolist.controller.it;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
public class ItemControllerIT {

    @Value("${TARGET_HOST:localhost}")
    private String targetHost;
    @Value("${TARGET_PORT:8100}")
    private String targetPort;

    private RestTemplate restTemplate;
    private HttpHeaders httpHeaders;

    @BeforeEach
    public void setUp() {
        this.restTemplate = new RestTemplate();
        this.httpHeaders = new HttpHeaders();
        List<MediaType> mediaTypes = new LinkedList<>();
        mediaTypes.add(MediaType.APPLICATION_JSON);
        this.httpHeaders.setAccept(mediaTypes);
    }

    @Test
    public void getTodoItems_admin() {
        String cookie = this.authenticate("admin", "admin");
        this.httpHeaders.add(HttpHeaders.COOKIE, cookie);
        HttpEntity<Void> request = new HttpEntity<>(httpHeaders);
        ResponseEntity<String> result = this.restTemplate.exchange("http://"+ targetHost +":"+targetPort+"/tasklist/api/v1/items", HttpMethod.GET, request, String.class);
        assertEquals(HttpStatus.OK, result.getStatusCode(), "Status");
        assertEquals("[{\"id\":1,\"parentId\":-1,\"dueDate\":null,\"dueTime\":null,\"dueTimezone\":null,\"title\":\"test\",\"description\":\"test\",\"children\":[],\"done\":false,\"scheduled\":false,\"repeat\":{\"times\":0,\"period\":\"None\"},\"lastNotification\":null}]", result.getBody(), "Body");
    }

    @Test
    public void getTodoItems_user() {
        String cookie = this.authenticate("user", "user");
        this.httpHeaders.add(HttpHeaders.COOKIE, cookie);
        HttpEntity<Void> request = new HttpEntity<>(httpHeaders);
        ResponseEntity<String> result = this.restTemplate.exchange("http://"+ targetHost +":"+targetPort+"/tasklist/api/v1/items", HttpMethod.GET, request, String.class);
        assertEquals(HttpStatus.OK, result.getStatusCode(), "Status");
        assertEquals("[]", result.getBody(), "Body"); //
    }

    @Test
    public void addNewTask_removeTask_user() {
        String cookie = this.authenticate("user", "user");
        this.httpHeaders.add(HttpHeaders.COOKIE, cookie);
        this.httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        String payload = "{ \"id\": -1, \"parentId\": -1, \"title\": \"new\", \"description\": \"integration-test\", \"done\": false, \"scheduled\": false, \"dueDate\": \"\", \"repeat\": { \"times\": 0, \"period\": \"None\" }, \"dueDate\": \"\", \"dueTime\": \"\", \"dueTimezone\": \"\"}";
        HttpEntity<String> addRequest = new HttpEntity<>(payload, httpHeaders);
        ResponseEntity<Integer> addResult = this.restTemplate.postForEntity("http://"+targetHost+":"+targetPort+"/tasklist/api/v1/items/add", addRequest, Integer.class);
        assertEquals(HttpStatus.OK, addResult.getStatusCode(), "Status");
        assertEquals(1, addResult.getBody(), "Body");
        HttpEntity<Void> itemsRequest = new HttpEntity<>(httpHeaders);
        ResponseEntity<String> itemsResult = this.restTemplate.exchange("http://"+ targetHost +":"+targetPort+"/tasklist/api/v1/items", HttpMethod.GET, itemsRequest, String.class);
        assertEquals(HttpStatus.OK, itemsResult.getStatusCode(), "Status");
        assertEquals("[{\"id\":1,\"parentId\":-1,\"dueDate\":null,\"dueTime\":null,\"dueTimezone\":null,\"title\":\"new\",\"description\":\"integration-test\",\"children\":[],\"done\":false,\"scheduled\":false,\"repeat\":{\"times\":0,\"period\":\"None\"},\"lastNotification\":null}]", itemsResult.getBody(), "Body");
        HttpEntity<Void> deleteRequest = new HttpEntity<>(httpHeaders);
        ResponseEntity<String> deleteResponse = this.restTemplate.exchange("http://"+targetHost+":"+targetPort+"/tasklist/api/v1/items/1/delete", HttpMethod.GET, deleteRequest, String.class);
        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode(), "Status");
        assertTrue(deleteResponse.getBody().equalsIgnoreCase("true"), "Body");
        itemsResult = this.restTemplate.exchange("http://"+ targetHost +":"+targetPort+"/tasklist/api/v1/items", HttpMethod.GET, itemsRequest, String.class);
        assertEquals(HttpStatus.OK, itemsResult.getStatusCode(), "Status");
        assertEquals("[]", itemsResult.getBody(), "Body");
    }

    private String authenticate(String username, String password) {
        this.httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", username);
        params.add("password", password);
        HttpEntity<MultiValueMap<String, String>> authRequest = new HttpEntity<>(params, this.httpHeaders);
        ResponseEntity<String> authResult = this.restTemplate.postForEntity("http://"+targetHost+":"+targetPort+"/tasklist/authentication", authRequest, String.class);
        assertEquals(HttpStatus.FOUND, authResult.getStatusCode(), "Auth status");
        assertFalse(authResult.getHeaders().getLocation().getPath().contains("login"), "No error");
        String session = null;
        for(String cookie: authResult.getHeaders().get(HttpHeaders.SET_COOKIE)) {
            if(cookie.contains("JSESSIONID")) {
                session = cookie.substring(0, 43);
            }
        }
        this.httpHeaders.setContentType(null);
        return session;
    }

}
