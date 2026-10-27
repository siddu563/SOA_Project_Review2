package com.learnknow.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnknow.course.CourseServiceApplication.CourseRequest;
import com.learnknow.course.CourseServiceApplication.ModuleRequest;
import com.learnknow.course.CourseServiceApplication.ResourceRequest;
import com.learnknow.course.model.Course;
import com.learnknow.course.repository.CourseRepository;
import com.learnknow.course.repository.ModuleRepository;
import com.learnknow.course.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CourseServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        resourceRepository.deleteAll();
        moduleRepository.deleteAll();
        courseRepository.deleteAll();
    }

    @Test
    void testCreateCourseSuccess() throws Exception {
        CourseRequest req = new CourseRequest("Spring Cloud Microservices", "Complete Guide to Cloud Native Java", "INTERMEDIATE");

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Spring Cloud Microservices")))
                .andExpect(jsonPath("$.level", is("INTERMEDIATE")));
    }

    @Test
    void testCreateCourseBlankTitleReturnsBadRequest() throws Exception {
        CourseRequest req = new CourseRequest("  ", "Description", "BEGINNER");

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.details", notNullValue()));
    }

    @Test
    void testGetAllCourses() throws Exception {
        Course c1 = new Course(); c1.title = "Course 1"; courseRepository.save(c1);
        Course c2 = new Course(); c2.title = "Course 2"; courseRepository.save(c2);

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Course 1")))
                .andExpect(jsonPath("$[1].title", is("Course 2")));
    }

    @Test
    void testGetCourseById() throws Exception {
        Course c = new Course(); c.title = "Docker & Kubernetes"; c.level = "ADVANCED";
        Course saved = courseRepository.save(c);

        mockMvc.perform(get("/api/courses/" + saved.id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.id.intValue())))
                .andExpect(jsonPath("$.title", is("Docker & Kubernetes")));
    }

    @Test
    void testGetNonExistentCourseReturns404() throws Exception {
        mockMvc.perform(get("/api/courses/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    void testUpdateCourse() throws Exception {
        Course c = new Course(); c.title = "Initial Title"; courseRepository.save(c);

        CourseRequest updateReq = new CourseRequest("Updated Title", "Updated Description", "ADVANCED");
        mockMvc.perform(put("/api/courses/" + c.id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Title")))
                .andExpect(jsonPath("$.description", is("Updated Description")))
                .andExpect(jsonPath("$.level", is("ADVANCED")));
    }

    @Test
    void testDeleteCourse() throws Exception {
        Course c = new Course(); c.title = "To Be Deleted"; courseRepository.save(c);

        mockMvc.perform(delete("/api/courses/" + c.id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/courses/" + c.id))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAddAndGetModules() throws Exception {
        Course c = new Course(); c.title = "Course with Modules"; courseRepository.save(c);

        ModuleRequest m1 = new ModuleRequest("Module 1: Basics", 1);
        mockMvc.perform(post("/api/courses/" + c.id + "/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(m1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Module 1: Basics")));

        mockMvc.perform(get("/api/courses/" + c.id + "/modules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Module 1: Basics")));
    }

    @Test
    void testAddAndGetResources() throws Exception {
        ResourceRequest r = new ResourceRequest("Java CheatSheet PDF", "PDF", "https://cdn.example.com/sheet.pdf", "PUBLIC");

        MvcResult res = mockMvc.perform(post("/api/courses/modules/10/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Java CheatSheet PDF")))
                .andExpect(jsonPath("$.resourceType", is("PDF")))
                .andReturn();

        mockMvc.perform(get("/api/courses/modules/10/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Java CheatSheet PDF")));
    }
}
