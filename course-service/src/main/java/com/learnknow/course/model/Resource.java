package com.learnknow.course.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Resource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long moduleId;
    @NotBlank public String title;
    public String resourceType;
    public String contentUrl;
    public String accessLevel = "ENROLLED";
}
