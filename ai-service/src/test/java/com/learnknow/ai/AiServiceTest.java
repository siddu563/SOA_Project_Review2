package com.learnknow.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnknow.ai.AiServiceApplication.AskRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AiServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InteractionRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testAskNullPointerExceptionQuestion() throws Exception {
        AskRequest req = new AskRequest(1L, "What causes a NullPointerException in Java?");

        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.question", is("What causes a NullPointerException in Java?")))
                .andExpect(jsonPath("$.response", containsString("NullPointerException")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void testAskRecursionQuestion() throws Exception {
        AskRequest req = new AskRequest(2L, "How does recursion work?");

        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response", containsString("base case")));
    }

    @Test
    void testAskRestApiQuestion() throws Exception {
        AskRequest req = new AskRequest(3L, "What is REST API design?");

        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response", containsString("HTTP")));
    }

    @Test
    void testAskGenericQuestion() throws Exception {
        AskRequest req = new AskRequest(4L, "How to debug my loop?");

        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response", notNullValue()));
    }

    @Test
    void testAskBlankQuestionReturnsBadRequest() throws Exception {
        AskRequest req = new AskRequest(1L, "   ");

        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.details", notNullValue()));
    }

    @Test
    void testAskNullUserIdReturnsBadRequest() throws Exception {
        AskRequest req = new AskRequest(null, "Valid question");

        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }
}
