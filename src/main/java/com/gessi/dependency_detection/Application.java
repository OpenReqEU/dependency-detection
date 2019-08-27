package com.gessi.dependency_detection;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import com.gessi.dependency_detection.service.DependencyService;
import com.gessi.dependency_detection.service.StorageProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class Application {
    public static void main(String[] args) throws Exception {

	System.setProperty("hadoop.home.dir", System.getProperty("user.dir"));
	SpringApplication.run(Application.class, args);

    }
    
    @Bean
    CommandLineRunner init(DependencyService storageService) {
		return args -> {
			storageService.deleteAll();
			storageService.init();
		};
    }
    
}
