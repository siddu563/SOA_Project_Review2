package com.learnknow.assignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnknow.assignment.AssignmentServiceApplication.AssignmentRequest;
import com.learnknow.assignment.AssignmentServiceApplication.SubmitRequest;
import com.learnknow.assignment.AssignmentServiceApplication.Assignment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AssignmentServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        submissionRepository.deleteAll();
        assignmentRepository.deleteAll();
    }

    @Test
    void testCreateAssignmentSuccess() throws Exception {
        AssignmentRequest req = new AssignmentRequest(10L, "Binary Search Tree", "Implement insert and search in Java", LocalDateTime.now().plusDays(5));

        mockMvc.perform(post("/api/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Binary Search Tree")))
                .andExpect(jsonPath("$.courseId", is(10)));
    }

    @Test
    void testCreateAssignmentBlankTitleReturnsBadRequest() throws Exception {
        AssignmentRequest req = new AssignmentRequest(10L, "", "Desc", null);

        mockMvc.perform(post("/api/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.details", notNullValue()));
    }

    @Test
    void testGetAssignmentById() throws Exception {
        Assignment a = new Assignment();
        a.courseId = 1L; a.title = "REST Controller"; a.description = "Create a controller";
        Assignment saved = assignmentRepository.save(a);

        mockMvc.perform(get("/api/assignments/" + saved.id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.id.intValue())))
                .andExpect(jsonPath("$.title", is("REST Controller")));
    }

    @Test
    void testGetNonExistentAssignmentReturns404() throws Exception {
        mockMvc.perform(get("/api/assignments/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    void testSubmitAssignmentSuccess() throws Exception {
        Assignment a = new Assignment();
        a.courseId = 1L; a.title = "Hello World";
        Assignment saved = assignmentRepository.save(a);

        SubmitRequest subReq = new SubmitRequest(1L, "System.out.println(\"Hello World!\");");

        mockMvc.perform(post("/api/assignments/" + saved.id + "/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.assignmentId", is(saved.id.intValue())))
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.status", is("SUBMITTED")))
                .andExpect(jsonPath("$.code", containsString("Hello World")));
    }

    @Test
    void testSubmitNonExistentAssignmentReturns404() throws Exception {
        SubmitRequest subReq = new SubmitRequest(1L, "some code");

        mockMvc.perform(post("/api/assignments/88888/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void testGetSubmissionByUserAndAssignment() throws Exception {
        Assignment a = new Assignment();
        a.courseId = 1L; a.title = "Algorithm Design";
        Assignment saved = assignmentRepository.save(a);

        SubmitRequest subReq = new SubmitRequest(7L, "public int sum(int a, int b) { return a + b; }");
        mockMvc.perform(post("/api/assignments/" + saved.id + "/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subReq)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/assignments/" + saved.id + "/submissions/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(7)))
                .andExpect(jsonPath("$.status", is("SUBMITTED")))
                .andExpect(jsonPath("$.code", containsString("return a + b;")));
    }
}
