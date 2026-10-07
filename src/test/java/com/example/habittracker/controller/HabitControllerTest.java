package com.example.habittracker.controller;

import com.example.habittracker.dto.HabitRequest;
import com.example.habittracker.model.Habit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HabitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private HabitRequest request(String name, String description) {
        HabitRequest request = new HabitRequest();
        request.setName(name);
        request.setDescription(description);
        request.setFrequency(Habit.Frequency.DAILY);
        return request;
    }

    @Test
    void createAndFetchHabit() throws Exception {
        HabitRequest request =
                request("Read", "Read 10 pages");

        String response = mockMvc.perform(
                        post("/api/habits")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Read"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Habit created = objectMapper.readValue(response, Habit.class);

        mockMvc.perform(get("/api/habits/" + created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Read"));
    }

    @Test
    void blankNameIsRejected() throws Exception {
        HabitRequest request =
                request("", "Description");

        mockMvc.perform(
                        post("/api/habits")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownHabitReturns404() throws Exception {
        mockMvc.perform(get("/api/habits/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
