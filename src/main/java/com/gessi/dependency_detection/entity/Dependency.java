package com.gessi.dependency_detection.entity;

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
}
