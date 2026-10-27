package com.learnknow.course;

import com.learnknow.course.model.Course;
import com.learnknow.course.model.Module;
import com.learnknow.course.model.Resource;
import com.learnknow.course.repository.CourseRepository;
import com.learnknow.course.repository.ModuleRepository;
import com.learnknow.course.repository.ResourceRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@SpringBootApplication
public class CourseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourseServiceApplication.class, args);
    }

    public record CourseRequest(
            @NotBlank(message = "Title must not be blank") String title,
            String description,
            String level
    ) {}

    public record ModuleRequest(
            @NotBlank(message = "Module title must not be blank") String title,
            Integer sequenceNo
    ) {}

    public record ResourceRequest(
            @NotBlank(message = "Resource title must not be blank") String title,
            String resourceType,
            String contentUrl,
            String accessLevel
    ) {}

    @RestController
    @RequestMapping("/api/courses")
    public static class CourseController {
        private final CourseRepository courses;
        private final ModuleRepository modules;
        private final ResourceRepository resources;

        public CourseController(CourseRepository courses, ModuleRepository modules, ResourceRepository resources) {
            this.courses = courses;
            this.modules = modules;
            this.resources = resources;
        }

        @GetMapping
        public List<Course> all() {
            return courses.findAll();
        }

        @GetMapping("/{id}")
        public Course one(@PathVariable("id") Long id) {
            return courses.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Course not found with id: " + id));
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public Course create(@Valid @RequestBody CourseRequest r) {
            Course c = new Course();
            c.title = r.title().trim();
            c.description = r.description();
            c.level = (r.level() == null || r.level().isBlank()) ? "BEGINNER" : r.level().toUpperCase();
            return courses.save(c);
        }

        @PutMapping("/{id}")
        public Course update(@PathVariable("id") Long id, @Valid @RequestBody CourseRequest r) {
            Course c = one(id);
            c.title = r.title().trim();
            c.description = r.description();
            if (r.level() != null && !r.level().isBlank()) {
                c.level = r.level().toUpperCase();
            }
            return courses.save(c);
        }

        @DeleteMapping("/{id}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void delete(@PathVariable("id") Long id) {
            Course c = one(id);
            courses.delete(c);
        }

        @GetMapping("/{courseId}/modules")
        public List<Module> getModules(@PathVariable("courseId") Long courseId) {
            one(courseId); // ensure course exists
            return modules.findByCourseIdOrderBySequenceNo(courseId);
        }

        @PostMapping("/{courseId}/modules")
        @ResponseStatus(HttpStatus.CREATED)
        public Module addModule(@PathVariable("courseId") Long courseId, @Valid @RequestBody ModuleRequest r) {
            one(courseId); // ensure course exists
            Module m = new Module();
            m.courseId = courseId;
            m.title = r.title().trim();
            m.sequenceNo = r.sequenceNo() != null ? r.sequenceNo() : 1;
            return modules.save(m);
        }

        @GetMapping("/modules/{moduleId}/resources")
        public List<Resource> getResources(@PathVariable("moduleId") Long moduleId) {
            return resources.findByModuleId(moduleId);
        }

        @PostMapping("/modules/{moduleId}/resources")
        @ResponseStatus(HttpStatus.CREATED)
        public Resource addResource(@PathVariable("moduleId") Long moduleId, @Valid @RequestBody ResourceRequest r) {
            Resource res = new Resource();
            res.moduleId = moduleId;
            res.title = r.title().trim();
            res.resourceType = (r.resourceType() == null || r.resourceType().isBlank()) ? "VIDEO" : r.resourceType().toUpperCase();
            res.contentUrl = r.contentUrl();
            res.accessLevel = (r.accessLevel() == null || r.accessLevel().isBlank()) ? "ENROLLED" : r.accessLevel().toUpperCase();
            return resources.save(res);
        }
    }
}
