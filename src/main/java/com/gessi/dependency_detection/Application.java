package com.gessi.dependency_detection;



import java.io.FileInputStream;
import java.io.InputStream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.gessi.dependency_detection.Application;

import com.gessi.dependency_detection.service.DependencyService;
import com.gessi.dependency_detection.service.StorageProperties;

import opennlp.tools.sentdetect.SentenceModel;

@SpringBootApplication
//@PropertySource({"classpath:application.properties"})
@EnableConfigurationProperties(StorageProperties.class)
public class Application {
    public static void main(String[] args) throws Exception {

	System.setProperty("hadoop.home.dir", System.getProperty("user.dir"));
	SpringApplication.run(Application.class, args);

    }
    
    @Bean
    CommandLineRunner init(DependencyService storageService) {
	return (args) -> {
	    storageService.deleteAll();
	    storageService.init();
	};
    }
    
}
