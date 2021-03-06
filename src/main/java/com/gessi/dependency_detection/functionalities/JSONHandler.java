package com.gessi.dependency_detection.functionalities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gessi.dependency_detection.components.Dependency;

public class JSONHandler {

	/**
	 * Constructor
	 */
	public JSONHandler() {
		super();

	}

	/**
	 * Read requirements from the selected project of the input JSON
	 * 
	 * @param jsonData
	 * @param projectId
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public Map<String, String> readRequirement(String jsonData, String projectId)
			throws JsonProcessingException, IOException {
		HashMap<String, String> requirms = new HashMap<>();

		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();

		// Get values
		JsonNode rootNode = objectMapper.readTree(jsonData);
		JsonNode projectNode = rootNode.get("projects");
		ArrayList<String> reqIds = new ArrayList<>();
		for (JsonNode child : projectNode) {
			String id = child.get("id").asText();
			if (id.equals(projectId)) {
				for (JsonNode r : child.get("specifiedRequirements")) {
					reqIds.add(r.asText());
				}
			}
		}
		JsonNode reqNode = rootNode.get("requirements");
		for (JsonNode child : reqNode) {
			String id = child.get("id").asText();
			if (reqIds.contains(id)) {
				String text = "";
				if (child.has("requirement_type")) {
					if (child.get("requirement_type").asText().equalsIgnoreCase("def")) {
						text = child.get("text").asText();
						requirms.put(id, text);
					}
				} else {
					text = child.get("text").asText();
					requirms.put(id, text);
				}
			}
		}
		return requirms;

	}

	/**
	 * Read all the dependencies from the JSON and return an array with all of them.
	 * 
	 * @param jsonData
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public List<ArrayList<Object>> readDependency(String jsonData) throws JsonProcessingException, IOException {
		ArrayList<ArrayList<Object>> deps = new ArrayList<>();

		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		// Get values
		JsonNode rootNode = objectMapper.readTree(jsonData);
		JsonNode depNode = rootNode.get("dependencies");
		if (depNode != null) {
			for (JsonNode child : depNode) {
				String dependencyType = child.get("dependency_type").asText();
				String status = child.get("status").asText();
				String from = child.get("fromid").asText();
				String to = child.get("toid").asText();
				JsonNode description = child.get("description");

				ArrayList<Object> dependency = new ArrayList<>();
				dependency.add(dependencyType);
				dependency.add(status);
				dependency.add(from);
				dependency.add(to);
				dependency.add(description);

				deps.add(dependency);
			}
		}
		return deps;

	}

	/**
	 * Store detected dependencies if they are not previously contained in the JSON.
	 * 
	 * @param jsonData
	 * @param newDeps
	 * @return
	 * @throws IOException
	 */
	public ObjectNode storeDependencies(String jsonData, List<Dependency> newDeps) throws IOException {
		List<ArrayList<Object>> deps = new ArrayList<>();
		ArrayList<ObjectNode> oldDeps = new ArrayList<>();
		// Read previous dependencies if any
		if (!jsonData.equals("")) {
			deps = readDependency(jsonData);
		}
		// create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();

		
		JsonNode rootNode = objectMapper.readTree(jsonData);
		ObjectNode objectNode = (ObjectNode) rootNode;
		ArrayNode depArrayNode = objectMapper.createArrayNode();

		// parse the old dependencies
		for (ArrayList<Object> node : deps) {
			ObjectNode objN = objectMapper.createObjectNode();
			objN.put("dependency_type", ((String) node.get(0)).toLowerCase());
			objN.put("status", ((String) node.get(1)).toLowerCase());
			objN.put("fromid", (String) node.get(2));
			objN.put("toid", (String) node.get(3));
			objN.set("description", (JsonNode) node.get(4));
			oldDeps.add(objN);
			depArrayNode.add(objN);
		}
		// Create new dependencies
		for (Dependency d : newDeps) {
			ObjectNode objN = objectMapper.createObjectNode();
			objN.put("dependency_type", d.getDependencyType().toString().toLowerCase());
			objN.put("status", d.getStatus().toString().toLowerCase());
			objN.put("fromid", d.getFrom());
			objN.put("toid", d.getTo());
			ArrayNode description = objectMapper.createArrayNode();
			ObjectNode node = objectMapper.createObjectNode();
			node.put("component", "dependency detection");
			description.add(node);
			objN.set("description", description);

			// Store new dependencies if they are not already contained in the JSON
			if (!isContained(oldDeps, objN)) {
				depArrayNode.add(objN);
			}

		}
		// Update the JSON with the dependencies (old and new)
		objectNode.set("dependencies", depArrayNode);

		return objectNode;
	}

	/**
	 * Check if a dependency is contained in a list of dependencies.
	 * @param oldDeps
	 * @param objN
	 * @return
	 */
	private boolean isContained(ArrayList<ObjectNode> oldDeps, ObjectNode objN) {

		for (ObjectNode node : oldDeps) {
			if (node.get("toid").asText().equals(objN.get("toid").asText())
					&& node.get("fromid").asText().equals(objN.get("fromid").asText())
					&& node.get("dependency_type").asText().equals(objN.get("dependency_type").asText())) {
				return true;
			}
		}
		return false;
	}

}
