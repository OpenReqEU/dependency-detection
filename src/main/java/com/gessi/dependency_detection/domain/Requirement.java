package com.gessi.dependency_detection.domain;

public class Requirement {
    String description;
    String id;

    public Requirement(String s, String s1) {
        description=s1;
        id=s;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
