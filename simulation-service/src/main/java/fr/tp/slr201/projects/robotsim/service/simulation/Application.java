package fr.tp.slr201.projects.robotsim.service.simulation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import fr.tp.inf112.projects.robotsim.model.FactoryJsonMapper;
import tools.jackson.databind.json.JsonMapper;

@SpringBootApplication
public class Application {

	@Bean
	@Primary
	public JsonMapper factoryJsonMapper() {
		return FactoryJsonMapper.createObjectMapper();
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
