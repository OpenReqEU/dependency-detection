package com.gessi.dependency_detection.components;

public class Dependency {

    private String	   from;
    private String	   to;
    private Status	   status;
    private DependencyType dependencyType;

    public Dependency() {
	super();
    }

    public Dependency(String from, String to, Status status, DependencyType dependencyType) {
	super();
	this.to = to;
	this.from = from;
	this.status = status;
	this.dependencyType = dependencyType;
    }
    


	public String getFrom() {
	return from;
    }

    public void setFrom(String from) {
	this.from = from;
    }

    public String getTo() {
	return to;
    }

    public void setTo(String to) {
	this.to = to;
    }

    public Status getStatus() {
	return status;
    }

    public void setStatus(Status status) {
	this.status = status;
    }

    public DependencyType getDependencyType() {
	return dependencyType;
    }

}
