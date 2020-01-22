package com.gessi.dependency_detection.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
public class RequirementEntity implements Serializable {

    @Id
    private String id;
    private String description;
    private String projectId;

    public RequirementEntity() {

    }

    public RequirementEntity(String id, String description, String projectId) {
        this.id = id;
        this.description = description;
        this.projectId = projectId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
