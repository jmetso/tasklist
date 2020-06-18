package net.metja.todolist.controller;

import net.metja.todolist.configuration.TestSecurityConfiguration;
import net.metja.todolist.database.DatabaseManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(RestController.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {TestSecurityConfiguration.class, RESTController.class})
public class RESTControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private DatabaseManager databaseManager;

    @Test
    @WithUserDetails("user")
    public void userUser() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/user")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.user", is("user")));
    }

    @Test
    @WithUserDetails("view")
    public void userView() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/user")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.user", is("view")));
    }

    @Test
    @WithUserDetails("admin")
    public void userAdmin() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/user")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.user", is("admin")));
    }

    @Test
    @WithUserDetails("user")
    public void hello() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/hello/world")
                    .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.hello", is("world!")));
    }

    @Test
    @WithAnonymousUser
    public void generatePassword() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/password/generate/password")
                .accept(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }

}