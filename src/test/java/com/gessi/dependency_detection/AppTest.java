package com.gessi.dependency_detection;

import com.gessi.dependency_detection.components.Dependency;
import com.gessi.dependency_detection.components.DependencyType;
import com.gessi.dependency_detection.components.Requirement;
import com.gessi.dependency_detection.components.Status;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.*;
import java.util.Arrays;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AppTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void Success() throws Exception {

        StringBuilder ontologyFile = new StringBuilder();
        StringBuilder jsonFile = new StringBuilder();
        String line;

        BufferedReader ontologyReader = new BufferedReader(new FileReader("src/test/java/com/gessi/dependency_detection/openreq-rail-small.owl"));
        while ((line = ontologyReader.readLine()) != null) {
            ontologyFile.append(line + "\n");
        }

        BufferedReader jsonReader = new BufferedReader(new FileReader("src/test/java/com/gessi/dependency_detection/test_dependencyDetection.json"));
        while ((line = jsonReader.readLine()) != null) {
            jsonFile.append(line + "\n");
        }

        MockMultipartFile ontology = new MockMultipartFile("ontology",
                "openreq-rail-small.owl",
                "text/plain",
                ontologyFile.toString().getBytes());

        MockMultipartFile json = new MockMultipartFile("json",
                "test_dependencyDetection.json",
                "application/json",
                jsonFile.toString().getBytes());

        this.mockMvc.perform(MockMvcRequestBuilders.fileUpload("/upc/dependency-detection/json/ontology/ABC?synonymy=false&threshold=0.1")
                .file(ontology)
                .file(json))
                .andExpect(status().isOk());
    }

    @Test
    public void FileFormatException() throws Exception {

        StringBuilder ontologyFile = new StringBuilder();
        StringBuilder jsonFile = new StringBuilder();
        String line;

        BufferedReader ontologyReader = new BufferedReader(new FileReader("src/test/java/com/gessi/dependency_detection/openreq-rail-small.owl"));
        while ((line = ontologyReader.readLine()) != null) {
            ontologyFile.append(line + "\n");
        }

        BufferedReader jsonReader = new BufferedReader(new FileReader("src/test/java/com/gessi/dependency_detection/test_dependencyDetection.json"));
        while ((line = jsonReader.readLine()) != null) {
            jsonFile.append(line + "\n");
        }

        MockMultipartFile ontology = new MockMultipartFile("ontology",
                "test_dependencyDetection.json",
                "text/plain",
                ontologyFile.toString().getBytes());

        MockMultipartFile json = new MockMultipartFile("json",
                "test_dependencyDetection.json",
                "application/json",
                jsonFile.toString().getBytes());

        this.mockMvc.perform(MockMvcRequestBuilders.fileUpload("/upc/dependency-detection/json/ontology/ABC?synonymy=true&threshold=0.1")
                .file(ontology)
                .file(json))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void EntityData() {
        final String from = "from";
        final String to = "to";
        Dependency dependency = new Dependency(from, to, Status.PROPOSED, DependencyType.CONFLICTS);
        Assert.assertEquals(dependency.getFrom(), from);
        Assert.assertEquals(dependency.getTo(), to);
        Assert.assertEquals(dependency.getStatus(), Status.PROPOSED);
        Assert.assertEquals(dependency.getDependencyType(), DependencyType.CONFLICTS);

        String clause = "Clause";
        int id = 123;
        Requirement requirement1 = new Requirement(id, clause);
        Requirement requirement2 = new Requirement(Arrays.asList(Integer.toString(id), clause));
        Assert.assertEquals(clause, requirement1.getClause());
        Assert.assertEquals(clause, requirement2.getClause());
        Assert.assertEquals(id, requirement1.getId());
        Assert.assertEquals(id, requirement1.getId());




    }
}
