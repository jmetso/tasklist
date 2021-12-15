package net.metja.todolist.controller;

import net.metja.todolist.configuration.TestSecurityConfiguration;
import net.metja.todolist.database.DatabaseManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(ListController.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {TestSecurityConfiguration.class, ListController.class})
public class ListControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private DatabaseManager databaseManager;

    @Test
    @WithUserDetails("user")
    public void addNewList() throws Exception {
        given(databaseManager.addList("user")).willReturn(1);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/new")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(true)));
    }

    @Test
    @WithUserDetails("user")
    public void addNewList_Fail() throws Exception {
        given(databaseManager.addList("user")).willReturn(-1);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/new")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$", is(false)));
    }

    @Test
    @WithUserDetails("view")
    public void addNewList_InappropriateRole() throws Exception {
        given(databaseManager.addList("view")).willReturn(1);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/new")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("admin")
    public void addNewList_AdminUser() throws Exception {
        given(databaseManager.addList("admin")).willReturn(1);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/new")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    public void addNewList_UnknownUser() throws Exception {
        given(databaseManager.addList("unknown")).willReturn(1);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/new")
                .with(httpBasic("unknown", "unknown"))
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    public void addNewList_AnonymousUser() throws Exception {
        given(databaseManager.addList("unknown")).willReturn(1);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/new")
                .with(httpBasic("unknown", "unknown"))
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isUnauthorized());
    }

}