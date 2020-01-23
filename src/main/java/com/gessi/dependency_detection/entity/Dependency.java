package com.gessi.dependency_detection.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import java.io.Serializable;

@Entity
@IdClass(DependencyId.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Dependency implements Serializable {

    @Id
    private String fromid;
    @Id
    private String toid;
    @Id
    private String dependency_type;
    @Id
    @JsonIgnore
    private Long analysis_id;
    @JsonIgnore
    private String project_id;

    public Dependency() {

    }

    public String getFromid() {
        return fromid;
    }

    public void setFromid(String fromid) {
        this.fromid = fromid;
    }

    public String getToid() {
        return toid;
    }

    public void setToid(String toid) {
        this.toid = toid;
    }

    public String getDependency_type() {
        return dependency_type;
    }

    public void setDependency_type(String dependency_type) {
        this.dependency_type = dependency_type;
    }

    public Long getAnalysis_id() {
        return analysis_id;
    }

    public void setAnalysis_id(Long analysis_id) {
        this.analysis_id = analysis_id;
    }

    public String getProject_id() {
        return project_id;
    }

    public void setProject_id(String project_id) {
        this.project_id = project_id;
    }
}
