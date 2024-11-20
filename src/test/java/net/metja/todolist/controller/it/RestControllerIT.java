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

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
public class RestControllerIT {

    @Value("${TARGET_HOST:localhost}")
    private String targetHost;
    @Value("${TARGET_PORT:8100}")
    private String targetPort;

    RestTemplate restTemplate;
    HttpHeaders httpHeaders;

    @BeforeEach
    public void setUp() {
        this.restTemplate = new RestTemplate();
        this.httpHeaders = new HttpHeaders();
        List<MediaType> mediaTypes = new LinkedList<>();
        mediaTypes.add(MediaType.APPLICATION_JSON);
        this.httpHeaders.setAccept(mediaTypes);
    }

    @Test
    public void testHello() {
        ResponseEntity<String> result = this.restTemplate.getForEntity("http://"+ targetHost +":"+targetPort+"/tasklist/api/v1/hello/integration-test", String.class);
        assertEquals(HttpStatus.OK, result.getStatusCode(), "Status");
        assertEquals("{\"hello\":\"integration-test!\"}", result.getBody(), "Body");
    }

    @Test
    public void getUser_admin() {
        String cookie = this.authenticate("admin", "admin");
        this.httpHeaders.add(HttpHeaders.COOKIE, cookie);
        HttpEntity<Void> request = new HttpEntity<>(httpHeaders);
        ResponseEntity<String> result = this.restTemplate.exchange("http://"+ targetHost +":"+targetPort+"/tasklist/api/v1/user", HttpMethod.GET, request, String.class);
        assertEquals(HttpStatus.OK, result.getStatusCode(), "Status");
        assertEquals("{\"user\": \"admin\"}", result.getBody(), "Body");
    }

    @Test
    public void getVersion() throws Exception {
        File f = new File("pom.xml");
        assertTrue(f.exists(), "pom.xml exists");
        Scanner reader = new Scanner(f);
        String version = null;
        int lineCounter = 1;
        while(reader.hasNextLine()) {
            String line = reader.nextLine();
            if(line.contains("version") && lineCounter > 2) {
                line = line.trim();
                version = line.substring(9, line.indexOf("<", 10));
                break;
            }
            ++lineCounter;
        }


        String cookie = this.authenticate("admin", "admin");
        this.httpHeaders.add(HttpHeaders.COOKIE, cookie);
        HttpEntity<Void> request = new HttpEntity<>(httpHeaders);
        ResponseEntity<String> result = this.restTemplate.exchange("http://"+ targetHost +":"+targetPort+"/tasklist/api/v1/version", HttpMethod.GET, request, String.class);
        assertEquals(HttpStatus.OK, result.getStatusCode(), "Status");
        assertEquals("{\"version\":\""+version+"\"}", result.getBody(), "Body");
    }

    private String authenticate(String username, String password) {
        this.httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", username);
        params.add("password", password);
        HttpEntity<MultiValueMap<String, String>> authRequest = new HttpEntity<>(params, this.httpHeaders);
        ResponseEntity<String> authResult = this.restTemplate.postForEntity("http://"+targetHost+":"+targetPort+"/tasklist/authentication", authRequest, String.class);
        assertEquals(HttpStatus.FOUND, authResult.getStatusCode(), "Auth status");
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
