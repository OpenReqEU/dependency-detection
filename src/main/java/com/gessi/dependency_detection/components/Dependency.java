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

    @Override
    public boolean equals(Object o) {

        if (o instanceof Dependency){
            Dependency dep = (Dependency) o;
            return (dep.getFrom().equals(this.from) && dep.getTo().equals(this.to) && dep.getDependencyType().equals(this.dependencyType));
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + (this.to != null ? this.to.hashCode() : 0) + (this.from != null ? this.from.hashCode() : 0) +
                (this.dependencyType != null ? this.dependencyType.hashCode() : 0);
        return hash;
    }

}
