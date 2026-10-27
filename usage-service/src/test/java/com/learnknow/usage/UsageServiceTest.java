package com.learnknow.usage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnknow.usage.UsageServiceApplication.ActivityRequest;
import com.learnknow.usage.UsageServiceApplication.ProgressRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class UsageServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProgressRepository progressRepository;

    @Autowired
    private HistoryRepository historyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        historyRepository.deleteAll();
        progressRepository.deleteAll();
    }

    @Test
    void testRecordActivitySuccess() throws Exception {
        ActivityRequest req = new ActivityRequest(1L, 100L, "READ");

        mockMvc.perform(post("/api/usage/activity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.resourceId", is(100)))
                .andExpect(jsonPath("$.action", is("READ")));
    }

    @Test
    void testRecordActivityBlankActionReturnsBadRequest() throws Exception {
        ActivityRequest req = new ActivityRequest(1L, 100L, "  ");

        mockMvc.perform(post("/api/usage/activity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.details", notNullValue()));
    }

    @Test
    void testGetActivityHistory() throws Exception {
        mockMvc.perform(post("/api/usage/activity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActivityRequest(2L, 10L, "VIEW"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/usage/activity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActivityRequest(2L, 20L, "COMPLETE"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/usage/history/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void testRecordProgressSuccess() throws Exception {
        ProgressRequest req = new ProgressRequest(1L, 10L, 5L, 75.5);

        mockMvc.perform(post("/api/usage/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.courseId", is(10)))
                .andExpect(jsonPath("$.completionPercent", is(75.5)));
    }

    @Test
    void testRecordProgressInvalidPercentReturnsBadRequest() throws Exception {
        ProgressRequest reqNegative = new ProgressRequest(1L, 10L, 5L, -5.0);

        mockMvc.perform(post("/api/usage/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqNegative)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));

        ProgressRequest reqOver100 = new ProgressRequest(1L, 10L, 5L, 120.0);

        mockMvc.perform(post("/api/usage/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqOver100)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void testGetProgressSuccess() throws Exception {
        ProgressRequest req = new ProgressRequest(3L, 30L, 1L, 90.0);
        mockMvc.perform(post("/api/usage/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/usage/progress/3/30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(3)))
                .andExpect(jsonPath("$.courseId", is(30)))
                .andExpect(jsonPath("$.completionPercent", is(90.0)));
    }

    @Test
    void testGetNonExistentProgressReturns404() throws Exception {
        mockMvc.perform(get("/api/usage/progress/999/888"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }
}
