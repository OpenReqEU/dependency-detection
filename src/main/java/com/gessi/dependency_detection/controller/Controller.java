package com.gessi.dependency_detection.controller;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutionException;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;

import com.gessi.dependency_detection.domain.KeywordTool;
import com.gessi.dependency_detection.util.Control;
import org.apache.uima.UIMAException;
import org.apache.uima.resource.ResourceInitializationException;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.gessi.dependency_detection.service.FileFormatException;

import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.LexicalSemanticResourceException;
import dkpro.similarity.algorithms.api.SimilarityException;

import com.fasterxml.jackson.databind.node.ObjectNode;
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

	/**
	 * Constructor
	 * 
	 * @param depService - The API service
	 */
	@Autowired
	public Controller(DependencyService depService) {
		this.depService = depService;
	}

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
	public ResponseEntity uploadJSONFile(
			@ApiParam(value = "The Ontology file to upload (RDF/XML lang.)", required = true) @RequestPart("ontology") @Valid @NotNull @NotBlank MultipartFile ontology,
			@ApiParam(value = "The JSON file to upload", required = true) @RequestPart("json") @Valid String json,
			@ApiParam(value = "Id of the project where the requirements to analize are.", required = true) @PathVariable("projectId") String projectId,
			@ApiParam(value = "If true, semantic similarity (synonymy) detection is applied to improve the detection algorithm.", required = false) @RequestParam(value = "synonymy", required = false,
			defaultValue = "false") Boolean synonymy,
			@ApiParam(value = "Threshold of semantic similarity to detect synonyms (included).", required = false) @RequestParam(value = "threshold", required = false) Double threshold,
			@ApiParam(value = "Keyword extraction tool (RULE_BASED or TFIDF_BASED)", required = false) @RequestParam(value = "keywordTool", required = false,
					defaultValue = "RULE_BASED") KeywordTool keywordTool)
			throws IOException, InterruptedException {
		Control.getInstance().showInfoMessage("Start computing");
		ObjectNode onjN = null;
		try {
			if (!ontology.getOriginalFilename().contains("owl") && !ontology.getOriginalFilename().contains("rdf")) {
				throw new FileFormatException();
			}

			// Initialize folders
			depService.initDoc();
			depService.initOnt();

			// save the ontology
			depService.store(ontology);

			// Ontology loader
			depService.loadOntology();

			depService.storeJson(json);
			// apply the dependency detection

			onjN = depService.conflictDependencyDetection(projectId, synonymy,
					threshold, keywordTool);

			/* Delete the uploaded file */
			depService.deleteAll();
		} catch (FileFormatException e) {
			return new ResponseEntity<>(createException(e.toString(),"The format file must be txt."), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (ResourceInitializationException e) {
			return new ResponseEntity<>(createException(e.toString(),"Parser Error"), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (UIMAException e) {
			return new ResponseEntity<>(createException(e.toString(),"NLP Error"), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (SimilarityException | LexicalSemanticResourceException e) {
			return new ResponseEntity<>(createException(e.toString(),"Similarity Error"), HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (ExecutionException e) {
			return new ResponseEntity<>(createException(e.toString(),"Execution Error"), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>(onjN, HttpStatus.OK);
	}

	private LinkedHashMap<String, String> createException(String exception, String message) {
		LinkedHashMap<String, String> result = new LinkedHashMap<>();
		result.put("status", "500");
		result.put("error", "Internal Server Error");
		result.put("exception", exception);
		result.put("message", message);
		return result;
	}

}
