package io.github.lelena1.travelbot;

import org.springframework.boot.SpringApplication;

public class TestTravelPlannerBotApplication {

	public static void main(String[] args) {
		SpringApplication.from(TravelPlannerBotApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
