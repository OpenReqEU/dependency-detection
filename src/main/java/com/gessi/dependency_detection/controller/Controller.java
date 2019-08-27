package com.gessi.dependency_detection.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;

import org.apache.uima.UIMAException;
import org.apache.uima.resource.ResourceInitializationException;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gessi.dependency_detection.service.FileFormatException;

import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.LexicalSemanticResourceException;
import dkpro.similarity.algorithms.api.SimilarityException;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gessi.dependency_detection.components.Dependency;
import com.gessi.dependency_detection.functionalities.OntologyHandler;
import com.gessi.dependency_detection.service.DependencyService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping("/upc/dependency-detection")
@Api(value = "ControllerAPI", produces = MediaType.APPLICATION_JSON_VALUE)
public class Controller {

	private final DependencyService depService;
	private static final String TABLE = "requirement";
	private static final String COLS = "id.text.";

	/**
	 * Constructor
	 * 
	 * @param depService - The API service
	 */
	@Autowired
	public Controller(DependencyService depService) {
		this.depService = depService;
	}

//    @PostMapping("/ontology")
//    @ApiOperation(value = "Uploads an ontology file", notes = "Uploads an ontology in RDF/XML language to the server to support the dependency detection.", response = String.class)
//    @ApiResponses(value = { @ApiResponse(code = 0, message = "Non content: There is no content to submit."),
//	    @ApiResponse(code = 200, message = "OK: The request has succeeded."),
//	    @ApiResponse(code = 201,
//		    message = "Created: The request has been fulfilled and has resulted in one or more new resources being created.",
//		    response = String.class),
//	    @ApiResponse(code = 401,
//		    message = "Unauthorized: The request has not been applied because it lacks valid authentication credentials for the target resource."),
//	    @ApiResponse(code = 403,
//		    message = "Forbidden: The server understood the request but refuses to authorize it."),
//	    @ApiResponse(code = 404,
//		    message = "Not Found: The server could not find what was requested by the client."),
//	    @ApiResponse(code = 500,
//		    message = "Internal Server Error. For more information see ‘message’ in the Response Body.") })
//    public ResponseEntity<?> uploadOwlFile(
//	    @ApiParam(value = "The file to upload (RDF/XML lang.)",
//		    required = true) @RequestParam("file") MultipartFile file,
//	    RedirectAttributes redirectAttributes) throws IOException, InterruptedException {
//
//	long startTime = System.currentTimeMillis();
//	long stopTime;
//	long elapsedTime;
//	LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
//	try {
//	    if (!file.getOriginalFilename().contains("owl") && !file.getOriginalFilename().contains("rdf")) {
//		throw new FileFormatException();
//	    }
//
//	    /* Delete the old ontology */
//	    depService.deleteAllOnt();
//	    depService.initOnt();
//
//	    // save the ontology
//	    depService.store(file, 1);
//	    redirectAttributes.addFlashAttribute("message",
//		    "You successfully uploaded " + file.getOriginalFilename() + "!");
//
//	    
//	    depService.loadOntology();
//
//	    // Save ontology remotely
//	    //depService.saveOntology();
//
//	    result.put("status", "200");
//	    result.put("message", "The ontology has been loaded.");
//	} catch (FileFormatException e) {
//	    result = new LinkedHashMap<String, String>();
//	    result.put("status", "500");
//	    result.put("error", "Internal Server Error");
//	    result.put("exception", e.toString());
//	    result.put("message", "The format file must be owl or rdf.");
//	    return new ResponseEntity<LinkedHashMap<String, String>>(result, HttpStatus.INTERNAL_SERVER_ERROR);
//	}
//	stopTime = System.currentTimeMillis();
//	elapsedTime = stopTime - startTime;
//	// System.out.println("[TIME] /file: " + timeFormat(elapsedTime) +
//	// "(mm:ss:mmss)");
//	return new ResponseEntity<LinkedHashMap<String, String>>(result, HttpStatus.OK);
//    }

	/**
	 * Function to upload an ontology (in RDF/XML language) and a JSON Object to the
	 * server, extracts the dependencies of all the project's requirements stored in
	 * JSON by the support of an ontology, NLP and ML algorithms and finally removes
	 * the uploaded files.
	 * 
	 * @param ontology
	 * @param json
	 * @param projectId
	 * @param synonymy
	 * @param threshold
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@PostMapping("/json/ontology/{projectId}")
	@ApiOperation(value = "Uploads JSON and Ontology files to detect dependencies", notes = "Uploads an ontology (in RDF/XML language) and a JSON Object to the server, extracts the dependencies of all the project's requirements stored in JSON by the support of the ontology and finally removes the uploaded files.", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 0, message = "Non content: There is no content to submit."),
			@ApiResponse(code = 200, message = "OK: The request has succeeded."),
			@ApiResponse(code = 201, message = "Created: The request has been fulfilled and has resulted in one or more new resources being created.", response = String.class),
			@ApiResponse(code = 401, message = "Unauthorized: The request has not been applied because it lacks valid authentication credentials for the target resource."),
			@ApiResponse(code = 403, message = "Forbidden: The server understood the request but refuses to authorize it."),
			@ApiResponse(code = 404, message = "Not Found: The server could not find what was requested by the client."),
			@ApiResponse(code = 500, message = "Internal Server Error. For more information see ‘message’ in the Response Body.") })
	public ResponseEntity<?> uploadJSONFile(
			@ApiParam(value = "The Ontology file to upload (RDF/XML lang.)", required = true) @RequestPart("ontology") @Valid @NotNull @NotBlank MultipartFile ontology,
			@ApiParam(value = "The JSON file to upload", required = true) @RequestPart("json") @Valid String json,
			@ApiParam(value = "Id of the project where the requirements to analize are.", required = true) @PathVariable("projectId") String projectId,
			@ApiParam(value = "If true, semantic similarity (synonymy) detection is applied to improve the detection algorithm.", required = true) @RequestParam(value = "synonymy", required = true) Boolean synonymy,
			@ApiParam(value = "Threshold of semantic similarity to detect synonyms (included).", required = false) @RequestParam(value = "threshold", required = false) Double threshold)
			throws IOException, InterruptedException {

		Instant start = Instant.now();
		long stopTime;
		long elapsedTime;
		ObjectNode onjN = null;
		try {
//	    }
			if (!ontology.getOriginalFilename().contains("owl") && !ontology.getOriginalFilename().contains("rdf")) {
				throw new FileFormatException();
			}

			// Initialize folders
			depService.initDoc();
			depService.initOnt();

			// save the ontology
			depService.store(ontology, 1);

			// Ontology loader
			depService.loadOntology();

			depService.storeJson(json);
			// apply the dependency detection

			onjN = depService.conflictDependencyDetection(projectId, synonymy,
					threshold);

			/* Delete the uploaded file */
			depService.deleteAll();
		} catch (FileFormatException e) {
			LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
			result.put("status", "500");
			result.put("error", "Internal Server Error");
			result.put("exception", e.toString());
			result.put("message", "The format file must be txt.");
			return new ResponseEntity<LinkedHashMap<String, String>>(result, HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (ResourceInitializationException e) {
			LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
			result.put("status", "500");
			result.put("error", "Internal Server Error");
			result.put("exception", e.toString());
			result.put("message", "Parser Error");
			return new ResponseEntity<LinkedHashMap<String, String>>(result, HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (UIMAException e) {
			LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
			result.put("status", "500");
			result.put("error", "Internal Server Error");
			result.put("exception", e.toString());
			result.put("message", "NLP Error");
			return new ResponseEntity<LinkedHashMap<String, String>>(result, HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (SimilarityException e) {
			LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
			result.put("status", "500");
			result.put("error", "Internal Server Error");
			result.put("exception", e.toString());
			result.put("message", "Similarity Error");
			return new ResponseEntity<LinkedHashMap<String, String>>(result, HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (LexicalSemanticResourceException e) {
			LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
			result.put("status", "500");
			result.put("error", "Internal Server Error");
			result.put("exception", e.toString());
			result.put("message", "Similarity Error");
			return new ResponseEntity<LinkedHashMap<String, String>>(result, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		Instant end = Instant.now();
		Duration timeElapsed = Duration.between(start, end);
//	System.out.println("Component Time: "+ timeElapsed.toMillis() +" millis.");
		return new ResponseEntity<ObjectNode>(onjN, HttpStatus.OK);
	}

}
