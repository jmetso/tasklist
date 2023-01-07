package net.metja.todolist.controller;

import net.metja.todolist.configuration.TestSecurityConfiguration;
import net.metja.todolist.database.DatabaseManager;
import net.metja.todolist.database.bean.Repeat;
import net.metja.todolist.database.bean.Todo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Janne Metso @copy; 2020
 * @since 2020-03-23
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(ItemController.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {TestSecurityConfiguration.class, ItemController.class})
public class ItemControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private DatabaseManager databaseManager;

    @Test
    @WithUserDetails("user")
    public void getTodoItemsAsUser() throws Exception {
        Todo todo = new Todo(1, -1, "Task");
        todo.setRepeat(new Repeat(1, Repeat.TimePeriod.Weeks));
        List<Todo> todoList = Arrays.asList(todo);

        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodos(1)).willReturn(todoList);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(todo.getId())))
                .andExpect(jsonPath("$[0].parentId", is(todo.getParentId())))
                .andExpect(jsonPath("$[0].title", is(todo.getTitle())))
                .andExpect(jsonPath("$[0].dueDate", nullValue()))
                .andExpect(jsonPath("$[0].dueTime", nullValue()))
                .andExpect(jsonPath("$[0].repeat", notNullValue()))
                .andExpect(jsonPath("$[0].repeat.times", is(1)))
                .andExpect(jsonPath("$[0].repeat.period", is("Weeks")))
                .andExpect(jsonPath("$[0].repeat", notNullValue()))
                .andExpect(jsonPath("$[0].description", nullValue()));
    }

    @Test
    @WithUserDetails("admin")
    public void getTodoItemsAsAdmin() throws Exception {
        Todo todo = new Todo(1, -1, "Task");
        todo.setRepeat(new Repeat(0, Repeat.TimePeriod.None));
        List<Todo> todoList = Arrays.asList(todo);

        given(databaseManager.getUserList("admin")).willReturn(1);
        given(databaseManager.getTodos(1)).willReturn(todoList);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(todo.getId())))
                .andExpect(jsonPath("$[0].parentId", is(todo.getParentId())))
                .andExpect(jsonPath("$[0].title", is(todo.getTitle())))
                .andExpect(jsonPath("$[0].dueDate", nullValue()))
                .andExpect(jsonPath("$[0].dueTime", nullValue()))
                .andExpect(jsonPath("$[0].repeat", notNullValue()))
                .andExpect(jsonPath("$[0].repeat.times", is(0)))
                .andExpect(jsonPath("$[0].repeat.period", is("None")))
                .andExpect(jsonPath("$[0].description", nullValue()));
    }

    @Test
    @WithUserDetails("view")
    public void getTodoItemsAsView() throws Exception {
        Todo todo = new Todo(1, -1, "Task");
        todo.setRepeat(new Repeat(0, Repeat.TimePeriod.None));
        List<Todo> todoList = Arrays.asList(todo);

        given(databaseManager.getUserList("view")).willReturn(1);
        given(databaseManager.getTodos(1)).willReturn(todoList);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(todo.getId())))
                .andExpect(jsonPath("$[0].parentId", is(todo.getParentId())))
                .andExpect(jsonPath("$[0].title", is(todo.getTitle())))
                .andExpect(jsonPath("$[0].dueDate", nullValue()))
                .andExpect(jsonPath("$[0].dueTime", nullValue()))
                .andExpect(jsonPath("$[0].description", nullValue()));
    }

    @Test
    public void getTodoItems_UnknownUser() throws Exception {
        given(databaseManager.getUserList("unknown")).willReturn(-1);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items")
                .with(httpBasic("unknown", "unknown"))
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails("user")
    public void getTodoItems_NoList() throws Exception {
        Todo todo = new Todo(1, -1, "Task");
        todo.setRepeat(new Repeat(0, Repeat.TimePeriod.None));
        List<Todo> todoList = Arrays.asList(todo);

        given(databaseManager.getUserList("user")).willReturn(-1);
        given(databaseManager.getTodos(1)).willReturn(todoList);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items")
                .with(httpBasic("user", "user"))
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("user")
    public void getTodoItems_NoItems() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodos(1)).willReturn(null);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items")
                .with(httpBasic("user", "user"))
                .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithUserDetails("user")
    public void addTodoListItemAsUser() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.addTodo(eq(1), any())).willReturn(1);

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/items/add")
                .content("{\"id\":1,\"parentId\":-1,\"Title\":\"Title\"}")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(1)));
    }

    @Test
    @WithUserDetails("user")
    public void failAddTodoListItemAsUser() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.addTodo(eq(1), any())).willReturn(-1);

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/items/add")
                .content("{\"id\":1,\"parentId\":-1,\"Title\":\"Title\"}")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithUserDetails("admin")
    public void addTodoListItemAsAdmin() throws Exception {
        given(databaseManager.getUserList("admin")).willReturn(1);
        given(databaseManager.addTodo(eq(1), any())).willReturn(1);

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/items/add")
                .content("{\"id\":1,\"parentId\":-1,\"Title\":\"Title\"}")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(1)));
    }

    @Test
    public void addTodoListItem_UnknownUser() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        given(databaseManager.getUserList("unknown")).willReturn(-1);
        given(databaseManager.addTodo(eq(1), any())).willReturn(1);

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/items/add")
                .content("{\"id\":1,\"parentId\":-1,\"Title\":\"Title\"}")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithUserDetails("view")
    public void addTodoListItem_InappropriateRole() throws Exception {
        given(databaseManager.getUserList("view")).willReturn(-1);
        given(databaseManager.addTodo(eq(1), any())).willReturn(1);

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/items/add")
                .content("{\"id\":1,\"parentId\":-1,\"Title\":\"Title\"}")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithUserDetails("user")
    public void deleteTodoListItemAsUser() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.deleteTodo(1, 1)).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/delete")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(true)));
    }

    @Test
    @WithUserDetails("admin")
    public void deleteTodoListItemAsAdmin() throws Exception {
        given(databaseManager.getUserList("admin")).willReturn(1);
        given(databaseManager.deleteTodo(1, 1)).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/delete")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(true)));
    }

    @Test
    @WithUserDetails("user")
    public void deleteTodoListItem_UnknownItem() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.deleteTodo(1, 1)).willReturn(false);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/delete")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$", is(false)));
    }

    @Test
    @WithUserDetails("user")
    public void deleteTodoListItem_UnknownList() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(-1);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/delete")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$", is(false)));
    }

    @Test
    public void deleteTodoListItem_UnknownUser() throws Exception {
        given(databaseManager.getUserList("unknown")).willReturn(1);
        given(databaseManager.deleteTodo(1, 1)).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/delete")
                .with(httpBasic("unknown", "unknown"))
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails("view")
    public void deleteTodoListItem_InappropriateRole() throws Exception {
        given(databaseManager.getUserList("view")).willReturn(1);
        given(databaseManager.deleteTodo(1, 1)).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/delete")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("user")
    public void updateTodoListItemAsUser() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(new Todo(1, -1, "Title"));
        given(databaseManager.updateTodo(eq(1), any())).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/items/1/update")
                .content("{\"id\":1,\"parentId\":-1,\"title\":\"New title\"}")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("admin")
    public void updateTodoListItemAsAdmin() throws Exception {
        given(databaseManager.getUserList("admin")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(new Todo(1, -1, "Title"));
        given(databaseManager.updateTodo(eq(1), any())).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/items/1/update")
                .content("{\"id\":1,\"parentId\":-1,\"title\":\"New title\"}")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("view")
    public void updateTodoListItem_InappropriateRole() throws Exception {
        given(databaseManager.getUserList("view")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(new Todo(1, -1, "Title"));
        given(databaseManager.updateTodo(eq(1), any())).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/items/1/update")
                .content("{\"id\":1,\"parentId\":-1,\"title\":\"New title\"}")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("user")
    public void updateTodoListItem_UnknownList() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(-1);

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/items/1/update")
                .content("{\"id\":1,\"parentId\":-1,\"title\":\"New title\"}")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("user")
    public void updateTodoListItem_UnknownItem() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(null);

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/items/1/update")
                .content("{\"id\":1,\"parentId\":-1,\"title\":\"New title\"}")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("user")
    public void updateTodoListItem_UnknownUser() throws Exception {
        given(databaseManager.getUserList("unknown")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(null);

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/items/1/update")
                .content("{\"id\":1,\"parentId\":-1,\"title\":\"New title\"}")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding("UTF-8")
                .with(httpBasic("unknown", "unknown"))
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails("user")
    public void markTodoListItemAsDoneAsUser() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(todo);
        given(databaseManager.updateTodo(1, todo)).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/done")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", is(true)));

        assertTrue(todo.isDone(), "Status");
    }

    @Test
    @WithUserDetails("user")
    public void markDailyRepeatingTodoListItemAsDoneAsUser() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.of(2019, 11, 30));
        todo.setRepeat(new Repeat(1, Repeat.TimePeriod.Days));

        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(todo);
        given(databaseManager.updateTodo(1, todo)).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/done")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", is(true)));

        assertFalse(todo.isDone(), "Status");
        assertTrue(LocalDate.of(2019, 12, 1).toString().equalsIgnoreCase(todo.getDueDate().toString()), "Due date");
    }

    @Test
    @WithUserDetails("user")
    public void markWeeklyRepeatingTodoListItemAsDoneAsUser() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.of(2019, 11, 30));
        todo.setRepeat(new Repeat(1, Repeat.TimePeriod.Weeks));

        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(todo);
        given(databaseManager.updateTodo(1, todo)).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/done")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", is(true)));

        assertFalse(todo.isDone(), "Status");
        assertTrue(LocalDate.of(2019, 12, 7).toString().equalsIgnoreCase(todo.getDueDate().toString()), "Due date");
    }

    @Test
    @WithUserDetails("user")
    public void markBiWeeklyRepeatingTodoListItemAsDoneAsUser() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.of(2019, 11, 30));
        todo.setRepeat(new Repeat(2, Repeat.TimePeriod.Weeks));

        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(todo);
        given(databaseManager.updateTodo(1, todo)).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/done")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", is(true)));

        assertFalse(todo.isDone(), "Status");
        assertTrue(LocalDate.of(2019, 12, 14).toString().equalsIgnoreCase(todo.getDueDate().toString()), "Due date");
    }

    @Test
    @WithUserDetails("user")
    public void markMonthlyRepeatingTodoListItemAsDoneAsUser() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.of(2019, 11, 30));
        todo.setRepeat(new Repeat(1, Repeat.TimePeriod.Months));

        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(todo);
        given(databaseManager.updateTodo(1, todo)).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/done")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", is(true)));

        assertFalse(todo.isDone(), "Status");
        assertTrue(LocalDate.of(2019, 12, 30).toString().equalsIgnoreCase(todo.getDueDate().toString()), "Due date");
    }

    @Test
    @WithUserDetails("user")
    public void markYearlyRepeatingTodoListItemAsDoneAsUser() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        todo.setScheduled(true);
        todo.setDueDate(LocalDate.of(2019, 11, 30));
        todo.setRepeat(new Repeat(1, Repeat.TimePeriod.Years));

        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(todo);
        given(databaseManager.updateTodo(1, todo)).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/done")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", is(true)));

        assertFalse(todo.isDone(), "Status");
        assertTrue(LocalDate.of(2020, 11, 30).toString().equalsIgnoreCase(todo.getDueDate().toString()), "Due date");
    }

    @Test
    @WithUserDetails("admin")
    public void markTodoListItemAsDoneAsAdmin() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        todo.setRepeat(new Repeat(0, Repeat.TimePeriod.None));
        given(databaseManager.getUserList("admin")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(todo);
        given(databaseManager.updateTodo(1, todo)).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/done")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", is(true)));

        assertTrue(todo.isDone(), "Status");
    }

    @Test
    @WithUserDetails("view")
    public void markTodoListItemAsDone_InappropriateRole() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        todo.setRepeat(new Repeat(0, Repeat.TimePeriod.None));
        given(databaseManager.getUserList("view")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(todo);
        given(databaseManager.updateTodo(1, todo)).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/done")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isForbidden());

        assertFalse(todo.isDone(), "Status");
    }

    @Test
    @WithUserDetails("user")
    public void markTodoListItemAsDone_UnknownItem() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(null);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/done")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("user")
    public void markTodoListItemAsDone_UnknownList() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(-1);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/done")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("user")
    public void activateRepeatingTodoListItemAsUser() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        todo.setDone(true);
        todo.setScheduled(true);
        todo.setRepeat(new Repeat(1, Repeat.TimePeriod.Weeks));
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(eq(1), eq(1))).willReturn(todo);
        given(databaseManager.updateTodo(1, todo)).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/activate")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", is(true)));

        assertFalse(todo.isDone(), "Status");
    }

    @Test
    @WithUserDetails("admin")
    public void activateRepeatingTodoListItemAsAdmin() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        todo.setDone(true);
        todo.setScheduled(true);
        todo.setRepeat(new Repeat(1, Repeat.TimePeriod.Weeks));
        given(databaseManager.getUserList("admin")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(todo);
        given(databaseManager.updateTodo(1, todo)).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/activate")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", is(true)));

        assertFalse(todo.isDone(), "Status");
    }

    @Test
    @WithUserDetails("user")
    public void activateRepeatingTodoListItem_UnknownUser() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(null);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/activate")
                .with(httpBasic("unknown", "unknown"))
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails("user")
    public void activateRepeatingTodoListItem_UnknownItem() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(null);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/activate")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("view")
    public void activateRepeatingTodoListItem_InappropriateRole() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        todo.setDone(false);
        todo.setScheduled(true);
        todo.setRepeat(new Repeat(1, Repeat.TimePeriod.Weeks));
        given(databaseManager.getUserList("view")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(todo);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/activate")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("user")
    public void activateRepeatingTodoListItem_UnknownList() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(-1);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/activate")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("user")
    public void activateNonRepeatingTodoListItemAsUser() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        todo.setDone(true);
        todo.setScheduled(true);
        todo.setRepeat(new Repeat(0, Repeat.TimePeriod.None));
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(eq(1), eq(1))).willReturn(todo);
        given(databaseManager.updateTodo(1, todo)).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/activate")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", is(true)));

        assertFalse(todo.isDone(), "Status");
    }

    @Test
    @WithUserDetails("admin")
    public void activateNonRepeatingTodoListItemAsAdmin() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        todo.setDone(true);
        todo.setScheduled(true);
        todo.setRepeat(new Repeat(0, Repeat.TimePeriod.None));
        given(databaseManager.getUserList("admin")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(todo);
        given(databaseManager.updateTodo(1, todo)).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/activate")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", is(true)));

        assertFalse(todo.isDone(), "Status");
    }

    @Test
    @WithUserDetails("user")
    public void activateNonRepeatingTodoListItem_UnknownUser() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(null);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/activate")
                .with(httpBasic("unknown", "unknown"))
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails("user")
    public void activateNonRepeatingTodoListItem_UnknownItem() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(null);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/activate")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("view")
    public void activateNonRepeatingTodoListItem_InappropriateRole() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        todo.setDone(true);
        todo.setScheduled(true);
        todo.setRepeat(new Repeat(0, Repeat.TimePeriod.None));
        given(databaseManager.getUserList("view")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(todo);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/activate")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("user")
    public void activateNonRepeatingTodoListItem_UnknownList() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(-1);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/activate")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("user")
    public void deactivateNonRepeatingTodoListItemAsUser() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        todo.setDone(false);
        todo.setScheduled(true);
        todo.setRepeat(new Repeat(0, Repeat.TimePeriod.None));
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(todo);
        given(databaseManager.updateTodo(1, todo)).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/deactivate")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());

        assertFalse(todo.isDone(), "Status");
    }

    @Test
    @WithUserDetails("admin")
    public void deactivateNonRepeatingTodoListItemAsAdmin() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        todo.setDone(false);
        todo.setScheduled(true);
        todo.setRepeat(new Repeat(0, Repeat.TimePeriod.None));
        given(databaseManager.getUserList("admin")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(todo);
        given(databaseManager.updateTodo(1, todo)).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/deactivate")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());

        assertFalse(todo.isDone(), "Status");
    }

    @Test
    @WithUserDetails("user")
    public void deactivateNonRepeatingTodoListItem_UnknownUser() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(null);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/deactivate")
                .with(httpBasic("unknown", "unknown"))
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails("user")
    public void deactivateNonRepeatingTodoListItem_UnknownItem() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(null);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/deactivate")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("view")
    public void deactivateNonRepeatingTodoListItem_InappropriateRole() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        todo.setDone(false);
        todo.setScheduled(true);
        todo.setRepeat(new Repeat(0, Repeat.TimePeriod.None));
        given(databaseManager.getUserList("view")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(todo);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/deactivate")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("user")
    public void deactivateNonRepeatingTodoListItem_UnknownList() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(-1);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/deactivate")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("user")
    public void deactivateRepeatingTodoListItemAsUser() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        todo.setDone(false);
        todo.setScheduled(true);
        todo.setRepeat(new Repeat(1, Repeat.TimePeriod.Weeks));
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(todo);
        given(databaseManager.updateTodo(1, todo)).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/deactivate")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", is(true)));

        assertTrue(todo.isDone(), "Status");
    }

    @Test
    @WithUserDetails("admin")
    public void deactivateRepeatingTodoListItemAsAdmin() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        todo.setDone(false);
        todo.setScheduled(true);
        todo.setRepeat(new Repeat(1, Repeat.TimePeriod.Weeks));
        given(databaseManager.getUserList("admin")).willReturn(1);
        given(databaseManager.getTodo(eq(1), eq(1))).willReturn(todo);
        given(databaseManager.updateTodo(1, todo)).willReturn(true);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/deactivate")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$", is(true)));

        assertTrue(todo.isDone(), "Status");
    }

    @Test
    @WithUserDetails("user")
    public void deactivateRepeatingTodoListItem_UnknownUser() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(null);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/deactivate")
                .with(httpBasic("unknown", "unknown"))
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithUserDetails("user")
    public void deactivateRepeatingTodoListItem_UnknownItem() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(null);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/deactivate")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("view")
    public void deactivateRepeatingTodoListItem_InappropriateRole() throws Exception {
        Todo todo = new Todo(1, -1, "Title");
        todo.setDone(false);
        todo.setScheduled(true);
        todo.setRepeat(new Repeat(1, Repeat.TimePeriod.Weeks));
        given(databaseManager.getUserList("view")).willReturn(1);
        given(databaseManager.getTodo(1, 1)).willReturn(todo);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/deactivate")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("user")
    public void deactivateRepeatingTodoListItem_UnknownList() throws Exception {
        given(databaseManager.getUserList("user")).willReturn(-1);

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/items/1/deactivate")
                .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());
    }

}