package com.gessi.dependency_detection.components;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "Requirement")
public class Requirement {

    private int	   id;
    private String text;

    public Requirement(int id, String clause) {
	super();
	this.text = clause;
	this.id = id;
    }

    public Requirement(List<String> components) {
	this.id = Integer.parseInt(components.get(0));
	this.text = components.get(1);
    }

    @ApiModelProperty(value = "Text")
    public String getClause() {
	return text;
    }

    @ApiModelProperty(value = "Identification number")
    public int getId() {
	return id;
    }

    public void setClause(String clause) {
	this.text = clause;
    }

    @Override
    public String toString() {
	return "Requirement [id=" + id + ",\n clause=" + text + "]";
    }

}
