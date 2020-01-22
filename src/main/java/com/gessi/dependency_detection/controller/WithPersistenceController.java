package com.gessi.dependency_detection.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gessi.dependency_detection.domain.KeywordTool;
import com.gessi.dependency_detection.entity.RequirementEntity;
import com.gessi.dependency_detection.service.DependencyService;
import com.gessi.dependency_detection.service.FileFormatException;
import com.gessi.dependency_detection.util.Control;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.LexicalSemanticResourceException;
import dkpro.similarity.algorithms.api.SimilarityException;
import io.swagger.annotations.*;
import org.apache.uima.UIMAException;
import org.apache.uima.resource.ResourceInitializationException;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/upc/dependency-detection/persistence")
@Api(value = "ControllerAPI", produces = MediaType.APPLICATION_JSON_VALUE)
public class WithPersistenceController {

    @Autowired
    private DependencyService depService;

    @PostMapping("/requirements")
    @ApiOperation(value = "Uploads a requirements dataset", notes = "Uploads a JSON object containing requirements data " +
            "and stores the requirements into the service database ", response = String.class)
    @ApiResponses(value = { @ApiResponse(code = 0, message = "Non content: There is no content to submit."),
            @ApiResponse(code = 200, message = "OK: The request has succeeded."),
            @ApiResponse(code = 201, message = "Created: The request has been fulfilled and has resulted in one or more new resources being created.", response = String.class),
            @ApiResponse(code = 401, message = "Unauthorized: The request has not been applied because it lacks valid authentication credentials for the target resource."),
            @ApiResponse(code = 403, message = "Forbidden: The server understood the request but refuses to authorize it."),
            @ApiResponse(code = 404, message = "Not Found: The server could not find what was requested by the client."),
            @ApiResponse(code = 500, message = "Internal Server Error. For more information see ‘message’ in the Response Body.") })
    public ResponseEntity uploadJSONFile(
            @ApiParam(value = "The JSON file to upload", required = true) @RequestPart("json") @Valid String json,
            @ApiParam(value = "Id of the project to store the requirements", required = true) @PathVariable("projectId") String projectId)
            throws IOException, InterruptedException {
        Control.getInstance().showInfoMessage("Start storing requirements");
        try {
            depService.saveRequirements(json, projectId);
        } catch (Exception e) {
            return new ResponseEntity<>(createException(e.toString(),"Execution Error"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/requirements")
    @ApiOperation(value = "Get requirements", notes = "Get all requirements of a specific project", response = String.class)
    @ApiResponses(value = { @ApiResponse(code = 0, message = "Non content: There is no content to submit."),
            @ApiResponse(code = 200, message = "OK: The request has succeeded."),
            @ApiResponse(code = 201, message = "Created: The request has been fulfilled and has resulted in one or more new resources being created.", response = String.class),
            @ApiResponse(code = 401, message = "Unauthorized: The request has not been applied because it lacks valid authentication credentials for the target resource."),
            @ApiResponse(code = 403, message = "Forbidden: The server understood the request but refuses to authorize it."),
            @ApiResponse(code = 404, message = "Not Found: The server could not find what was requested by the client."),
            @ApiResponse(code = 500, message = "Internal Server Error. For more information see ‘message’ in the Response Body.") })
    public ResponseEntity getRequirements(
            @ApiParam(value = "Id of the project where the requirements to analize are.", required = true) @PathVariable("projectId") String projectId)
            throws IOException, InterruptedException {
        List<RequirementEntity> requirementEntities;
        try {
            requirementEntities = depService.getRequirements(projectId);
        } catch (Exception e) {
            return new ResponseEntity<>(createException(e.toString(),"Execution Error"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(requirementEntities, HttpStatus.OK);
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
