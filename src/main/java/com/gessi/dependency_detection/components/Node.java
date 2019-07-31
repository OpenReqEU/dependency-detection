package com.gessi.dependency_detection.components;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * 
 * Class used for a node of a dependency tree
 *
 */
public class Node {
	private int id;
	private int parentId;
	private Node parentNode;
	private List<Node> sonNodes;
	private String posTag;
	private String dependencyType;
	private String term;
	private String lemma;
	private Dependency dependency;
	
	public Node(int id, int parentId, String posTag, String dependencyType, String term, String lemma,
			Dependency dependency) {
		super();
		this.id = id;
		this.parentId = parentId;
		this.posTag = posTag;
		this.dependencyType = dependencyType;
		this.term = term;
		this.lemma = lemma;
		this.dependency = dependency;
		this.sonNodes = new ArrayList<>();
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getParentId() {
		return parentId;
	}

	public void setParentId(int parentId) {
		this.parentId = parentId;
	}

	public Node getParentNode() {
		return parentNode;
	}

	public void setParentNode(Node parentNode) {
		this.parentNode = parentNode;
	}
	
	public List<Node> getSonNodes() {
		return sonNodes;
	}

	public void setSonNodes(List<Node> sonNodes) {
		this.sonNodes = sonNodes;
	}
	
	public void addSonNodes(Node son) {
		this.sonNodes.add(son);
	}

	public String getPosTag() {
		return posTag;
	}

	public void setPosTag(String posTag) {
		this.posTag = posTag;
	}

	public String getDependencyType() {
		return dependencyType;
	}

	public void setDependencyType(String dependencyType) {
		this.dependencyType = dependencyType;
	}

	public String getTerm() {
		return term;
	}

	public void setTerm(String term) {
		this.term = term;
	}

	public String getLemma() {
		return lemma;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public Dependency getDependency() {
		return dependency;
	}

	public void setDependency(Dependency dependency) {
		this.dependency = dependency;
	}
	
	
	
}
