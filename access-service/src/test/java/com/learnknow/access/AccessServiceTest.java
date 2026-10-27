package com.learnknow.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnknow.access.AccessServiceApplication.EnrollRequest;
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
public class AccessServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EnrollmentRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testEnrollUserSuccess() throws Exception {
        EnrollRequest req = new EnrollRequest(1L, 100L);

        mockMvc.perform(post("/api/access/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.courseId", is(100)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    void testEnrollNullUserReturnsBadRequest() throws Exception {
        EnrollRequest req = new EnrollRequest(null, 100L);

        mockMvc.perform(post("/api/access/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.details", notNullValue()));
    }

    @Test
    void testCheckEnrollmentWhenEnrolled() throws Exception {
        EnrollRequest req = new EnrollRequest(2L, 200L);
        mockMvc.perform(post("/api/access/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/access/check/2/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(2)))
                .andExpect(jsonPath("$.courseId", is(200)))
                .andExpect(jsonPath("$.enrolled", is(true)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    void testCheckEnrollmentWhenNotEnrolled() throws Exception {
        mockMvc.perform(get("/api/access/check/999/888"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(999)))
                .andExpect(jsonPath("$.courseId", is(888)))
                .andExpect(jsonPath("$.enrolled", is(false)))
                .andExpect(jsonPath("$.status", is("NOT_ENROLLED")));
    }

    @Test
    void testValidateEndpoint() throws Exception {
        EnrollRequest req = new EnrollRequest(3L, 300L);
        mockMvc.perform(post("/api/access/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/access/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled", is(true)));
    }

    @Test
    void testGetUserEnrollments() throws Exception {
        mockMvc.perform(post("/api/access/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollRequest(5L, 101L))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/access/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollRequest(5L, 102L))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/access/user/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}
