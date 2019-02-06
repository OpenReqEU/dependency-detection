package com.gessi.dependency_detection.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.uima.UIMAException;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.similarity.algorithms.api.SimilarityException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gessi.dependency_detection.components.Dependency;
import com.gessi.dependency_detection.components.Node;
import com.gessi.dependency_detection.functionalities.JSONHandler;
import com.gessi.dependency_detection.functionalities.NLPAnalyser;
import com.gessi.dependency_detection.functionalities.OntologyHandler;
import com.gessi.dependency_detection.service.StorageException;
import com.hp.hpl.jena.ontology.OntModel;

import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.LexicalSemanticResourceException;

@Service
public class DependencyService {

	private final Path rootLocation;
	private final Path ontLocation;
	private final Path docLocation;
	private NLPAnalyser analizer;
	private ArrayList<Object> clauseList;
	private String jsonName;

	private String json;
	private String ontologyName;
	private OntModel model;
	private OntologyHandler ontHandler;
	private JSONHandler jsonHandler;

	/**
	 * Constructor
	 * 
	 * @param properties
	 * @throws IOException
	 */
	@Autowired
	public DependencyService(StorageProperties properties) throws IOException {
		this.rootLocation = Paths.get(properties.getRootLocation());
		this.ontLocation = Paths.get(properties.getOntLocation());
		this.docLocation = Paths.get(properties.getDocLocation());
		this.analizer = new NLPAnalyser();
		this.ontHandler = new OntologyHandler();
		this.jsonHandler = new JSONHandler();
	}

	public String getJson() {
		return json;
	}

	/**
	 * Function to delete the rootLocation path and all its files.
	 */
	public void deleteAll() {
		FileSystemUtils.deleteRecursively(rootLocation.toFile());
	}

	/**
	 * Function to delete the ontLocation path and all its files.
	 */
	public void deleteAllOnt() {
		FileSystemUtils.deleteRecursively(ontLocation.toFile());
	}

	/**
	 * Function to delete the docLocation path and all its files.
	 */
	public void deleteAllDoc() {
		FileSystemUtils.deleteRecursively(docLocation.toFile());
	}

	/**
	 * Function to create the rootLocation path.
	 */
	public void init() {
		try {
			Files.createDirectories(rootLocation);
		} catch (IOException e) {
			throw new StorageException("Could not initialize storage", e);
		}
	}

	/**
	 * Function to create the rootLocation path.
	 */
	public void initOnt() {
		try {
			Files.createDirectories(ontLocation);
		} catch (IOException e) {
			throw new StorageException("Could not initialize storage", e);
		}
	}

	/**
	 * Function to create the rootLocation path.
	 */
	public void initDoc() {
		try {
			Files.createDirectories(docLocation);
		} catch (IOException e) {
			throw new StorageException("Could not initialize storage", e);
		}
	}

	/**
	 * Function to store a file into rootLocation.
	 * 
	 * @param file
	 */
	public void store(MultipartFile file, int option) {
		String filename = "";
		if (option == 0 || option == 2) {
			filename = StringUtils.cleanPath(file.getOriginalFilename());
			this.jsonName = filename;
		} else if (option == 1) {
			filename = StringUtils.cleanPath(file.getOriginalFilename());
			this.ontologyName = filename;
		} else {
			filename = StringUtils.cleanPath(file.getOriginalFilename());
			throw new StorageException("Failed to store file " + filename);
		}
		try {
			if (file.isEmpty()) {
				throw new StorageException("Failed to store empty file " + filename);
			}
			if ((option == 0 || option == 2) && this.jsonName.contains("..")) {
				// This is a security check
				throw new StorageException(
						"Cannot store file with relative path outside current directory " + filename);
			} else if (option == 1 && this.ontologyName.contains("..")) {
				// This is a security check
				throw new StorageException(
						"Cannot store file with relative path outside current directory " + filename);
			}
			switch (option) {
			case 0:
				Files.copy(file.getInputStream(), this.rootLocation.resolve(filename),
						StandardCopyOption.REPLACE_EXISTING);
				break;
			case 1:
				Files.copy(file.getInputStream(), this.ontLocation.resolve(filename),
						StandardCopyOption.REPLACE_EXISTING);
				break;
			case 2:
				Files.copy(file.getInputStream(), this.docLocation.resolve(filename),
						StandardCopyOption.REPLACE_EXISTING);
				break;
			}

		} catch (IOException e) {
			throw new StorageException("Failed to store file " + filename, e);
		}
	}

	/**
	 * Function to store JSON String.
	 * 
	 * @param json
	 */
	public void storeJson(String json) {
		this.json = json;
		this.jsonName = "json-file";
		// clauseExtraction.setFilename(this.filename);
	}

	/**
	 * Function to load the stored file.
	 * 
	 * @param filename
	 * @return
	 */
	public Path load(String filename, int option) {
		switch (option) {
		case 0:
			return rootLocation.resolve(filename);
		case 1:
			return ontLocation.resolve(filename);
		case 2:
			return docLocation.resolve(filename);
		}
		return null;
	}

	/**
	 * Function to load the ontology
	 * 
	 * @throws IOException
	 */
	public void loadOntology() throws IOException {
		String source = "";
		ArrayList<String> fileLines = analizer.readFile(load(this.ontologyName, 1).toString());
		boolean find = false;
		int i = 0;
		while (!find && i < fileLines.size()) {
			if (fileLines.get(i).contains("xml:base=")) {
				source = fileLines.get(i).replaceAll("\\s+(xml:base=)", "").replaceAll("\"", "");
				find = true;
			}
			i++;
		}
		this.ontHandler.loadOnt(source, load(this.ontologyName, 1).toUri().toString());
	}

	// public void saveOntology() {
	//
	// }

	// public ArrayList<Object> extractClauseList(String dbName, String projectName,
	// String tableName, String colsName,
	// JdbcTemplate jdbcTemplate) throws IOException, ClassNotFoundException,
	// SQLException {
	// this.clauseList = utils.databaseExtractionJDBC(dbName, tableName, colsName,
	// projectName, jdbcTemplate);
	// return clauseList;
	// }

//	public ObjectNode conflictDependencyDetection(String projectId) throws IOException, ResourceInitializationException, UIMAException {
//		// ArrayList<String> fileLines = analizer.readFile(load(this.jsonName,
//		// 2).toString());
////	HashMap<String, String> requirements = jsonHandler.readRequirement(load(this.jsonName, 2).toString(),
////		projectId);
//		ontHandler.searchClasses(analizer);
//		HashMap<String, String> requirements = jsonHandler.readRequirement(json, projectId);
////		analizer.intiReqList();
//		for (Entry<String, String> entry : requirements.entrySet()) {
//			String key = entry.getKey();
//			String value = entry.getValue();
//			// System.out.println("\n\n/***************************NewRequirment***************************/");
//			// System.out.println("id: " + key);
//			// if(i == 7)
//			ArrayList<ArrayList<String>> parsedSent = analizer.applyNLPAnalisis(value);
//			ArrayList<String> words = new ArrayList<>();
//			for (ArrayList<String> senParse : parsedSent) {
//				// join subject action and object in the same array
//				for (String word : senParse) {
////					List<String> list = Arrays.asList(parses.split(","));
////					for (String l : list) {
//					words.add(word.toLowerCase());
////					}
//					// words.addAll(list);
//				}
//				// System.out.println("\nMatching");
//				ontHandler.matching(words, key, value, analizer);
//			}
//
//		}
////		analizer.writeFile(clauseList, "./src/main/resources/reqstree.txt");
////		try {
////			analizer.dependencyParser();
//////			analizer.semanticSimilarity();
////		} catch (ResourceInitializationException e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
////		} catch (UIMAException e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
////		}
//	
//		// System.out.println("\nConflict Detection:");
//		ArrayList<Dependency> deps = ontHandler.ontConflictDetection();
//		ObjectNode objN = jsonHandler.storeDependencies(json, deps);
//		return objN;
//	}

	/**
	 * Function to extract dependencies from requirements, with the support of the
	 * input ontology and NLP algorithms
	 * 
	 * @param projectId
	 * @param syny
	 * @param thr
	 * @return
	 * @throws IOException
	 * @throws ResourceInitializationException
	 * @throws UIMAException
	 * @throws                                  dkpro.similarity.algorithms.api.SimilarityException
	 * @throws LexicalSemanticResourceException
	 */
	public ObjectNode conflictDependencyDetection(String projectId, boolean syny, double thr)
			throws IOException, ResourceInitializationException, UIMAException,
			dkpro.similarity.algorithms.api.SimilarityException, LexicalSemanticResourceException {
		Runtime.getRuntime().gc();
		
		// analyse the ontology classes
		ontHandler.searchClasses(analizer);
		// read the requirements from JSON
		HashMap<String, String> requirements = jsonHandler.readRequirement(json, projectId);
		int time = 0;
		// foreach requirement
		for (Entry<String, String> entry : requirements.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
//			System.out.println("\nNew Requiremnet:" + value); 
			if (!key.equals(null) && !value.equals(null) && !value.equals("")) {
				// Apply NLP methods (syntactic approach)
				ArrayList<Node> syntxResutls = analizer.RequirementAnalisis(value);
				
				// Matching of extracted terms with the ontology, it is also applied the semantic appraoch
				ontHandler.matching(syntxResutls, key, value, analizer, syny, thr);

//				if (time % 100 == 0)
//					System.out.println("iteration:" + time);
//				time++;
			}
		}
		// Extract dependencies from the ontology
		ArrayList<Dependency> deps = ontHandler.ontConflictDetection();
		ObjectNode objN = jsonHandler.storeDependencies(json, deps);
		return objN;
	}
}
