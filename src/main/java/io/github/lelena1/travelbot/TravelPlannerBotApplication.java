package io.github.lelena1.travelbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TravelPlannerBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(TravelPlannerBotApplication.class, args);
	}

}
