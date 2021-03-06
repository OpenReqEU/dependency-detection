package com.gessi.dependency_detection.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import com.gessi.dependency_detection.WordEmbedding;
import com.gessi.dependency_detection.domain.KeywordTool;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.ResourceLoaderException;
import org.apache.uima.UIMAException;
import org.apache.uima.resource.ResourceInitializationException;
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

import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.LexicalSemanticResourceException;

@Service
public class DependencyService {

	private final Path rootLocation;
	private final Path ontLocation;
	private final Path docLocation;
	private static final String errorMessage = "Could not initialize storage";
	private NLPAnalyser analizer;

	private String json;
	private String ontologyName;
	private OntologyHandler ontHandler;
	private JSONHandler jsonHandler;

	/**
	 * Constructor
	 * 
	 * @param properties
	 * @throws IOException
	 */
	@Autowired
	public DependencyService(StorageProperties properties) throws IOException, ResourceLoaderException {
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
	 * Function to create the rootLocation path.
	 */
	public void init() {
		try {
			Files.createDirectories(rootLocation);
		} catch (IOException e) {
			throw new StorageException(errorMessage, e);
		}
	}

	/**
	 * Function to create the rootLocation path.
	 */
	public void initOnt() {
		try {
			Files.createDirectories(ontLocation);
		} catch (IOException e) {
			throw new StorageException(errorMessage, e);
		}
	}

	/**
	 * Function to create the rootLocation path.
	 */
	public void initDoc() {
		try {
			Files.createDirectories(docLocation);
		} catch (IOException e) {
			throw new StorageException(errorMessage, e);
		}
	}

	/**
	 * Function to store a file into rootLocation.
	 * 
	 * @param file
	 */
	public void store(MultipartFile file) {
		String filename = StringUtils.cleanPath(file.getOriginalFilename());
		this.ontologyName = filename;

		try {
			if (file.isEmpty()) {
				throw new StorageException("Failed to store empty file " + filename);
			}
			Files.copy(file.getInputStream(), this.ontLocation.resolve(filename),
					StandardCopyOption.REPLACE_EXISTING);
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
	    System.out.println(json);
		this.json = json;
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
		default:
			//not possible
			break;
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
		List<String> fileLines = analizer.readFile(load(this.ontologyName, 1).toString());
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
	 * @throws dkpro.similarity.algorithms.api.SimilarityException
	 * @throws LexicalSemanticResourceException
	 */
	public ObjectNode conflictDependencyDetection(String projectId, boolean syny, Double thr, KeywordTool keywordTool)
			throws IOException, ResourceInitializationException, UIMAException,
			dkpro.similarity.algorithms.api.SimilarityException, LexicalSemanticResourceException, ExecutionException, InterruptedException {

		// analyse the ontology classes
		if (keywordTool.equals(KeywordTool.TFIDF_BASED))  ontHandler.searchClassesTfIdfBased();
		else ontHandler.searchClasses(analizer);
		// read the requirements from JSON
		Map<String, String> requirements = jsonHandler.readRequirement(json, projectId);
		// foreach requirement

		List<Dependency> deps = new ArrayList<>();
		if (keywordTool.equals(KeywordTool.TFIDF_BASED)) {
			Map<String, String> syntxResutls = analizer.prepareRequirements(requirements);
			WordEmbedding wordEmbedding = new WordEmbedding();// Declared here so it won't initialize every time
			for (Entry<String, String> entry : requirements.entrySet()) {
				ontHandler.matching(syntxResutls.get(entry.getKey()), entry.getKey(), entry.getValue(), syny, thr, wordEmbedding);
			}
			// Extract dependencies from the ontology
			deps = ontHandler.ontConflictDetection();
		}

		else if (keywordTool.equals(KeywordTool.RULE_BASED)) {
			for (Entry<String, String> entry : requirements.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				if (key != null && value != null && !value.equals("")) {
					// Apply NLP methods (syntactic approach)
					List<Node> syntxResutls = analizer.requirementAnalysis(value);

					// Matching of extracted terms with the ontology, it is also applied the semantic appraoch
					ontHandler.matchingRuleBased(syntxResutls, key, value, analizer, syny, thr);
				}
			}
			// Extract dependencies from the ontology
			deps = ontHandler.ontConflictDetection();
		}
		System.out.println("DEPENDENCIES FOUND: "+deps.size());
		return jsonHandler.storeDependencies(json, deps);
	}
}
