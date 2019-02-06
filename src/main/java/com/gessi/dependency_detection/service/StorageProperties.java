package com.gessi.dependency_detection.service;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("storage")
public class StorageProperties {

    /**
     * Folder location for storing files
     */
    private String rootLocation	= "upload-dir";
    private String ontLocation	= "ontology";
    private String docLocation	= "documents";

    public String getRootLocation() {
	Path userDir = Paths.get(System.getProperty("user.dir"));
	return userDir.resolve(rootLocation).toString();
    }

    public String getOntLocation() {
	Path userDir = Paths.get(getRootLocation());
	return userDir.resolve(ontLocation).toString();
    }

    public String getDocLocation() {
	Path userDir = Paths.get(getRootLocation());
	return userDir.resolve(docLocation).toString();
    }

    public void setOntLocation(String ontLocation) {
	this.ontLocation = ontLocation;
    }

    public void setDocLocation(String docLocation) {
	this.docLocation = docLocation;
    }

    public void setRootLocation(String rootLocation) {
	this.rootLocation = rootLocation;
    }

}