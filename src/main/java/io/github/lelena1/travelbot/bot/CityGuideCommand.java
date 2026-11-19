package io.github.lelena1.travelbot.bot;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import io.github.lelena1.travelbot.places.CityGuideService;
import io.github.lelena1.travelbot.places.ItineraryService;

/**
 * Разделы путеводителя: «🏛 Места», «🍽 Рестораны», «🎟 Экскурсии», «🗓 План».
 * Каждый раздел — кнопка меню, команда и выбор поездки со своим значком.
 */
@Component
public class CityGuideCommand {

	private final TripChooser chooser;
	private final Map<MainMenu, TripChooser.Section> byMenu;
	private final Map<String, TripChooser.Section> byCommand;

	public CityGuideCommand(TripChooser chooser, CityGuideService guide, ItineraryService itinerary) {
		this.chooser = chooser;
		TripChooser.Section places = new TripChooser.Section("🏛", "Для какой поездки показать достопримечательности?",
				MainMenu.PLACES, guide::sights);
		TripChooser.Section restaurants = new TripChooser.Section("🍽", "Для какой поездки искать рестораны?",
				MainMenu.RESTAURANTS, guide::restaurants);
		TripChooser.Section tours = new TripChooser.Section("🎟", "Для какой поездки искать экскурсии?",
				MainMenu.TOURS, guide::tours);
		TripChooser.Section plan = new TripChooser.Section("🗓", "Для какой поездки составить план?",
				MainMenu.ITINERARY, itinerary::forTrip);
		this.byMenu = Map.of(MainMenu.PLACES, places, MainMenu.RESTAURANTS, restaurants, MainMenu.TOURS, tours,
				MainMenu.ITINERARY, plan);
		this.byCommand = Map.of("/places", places, "/restaurants", restaurants, "/tours", tours, "/plan", plan);
	}

	/** Кнопка главного меню одного из разделов. */
	public Optional<BotReply> menu(long userId, MainMenu item) {
		return Optional.ofNullable(byMenu.get(item)).map(section -> chooser.choose(userId, section));
	}

	/** «/places» или «/places 2». */
	public Optional<BotReply> command(long userId, String command, Optional<Long> number) {
		TripChooser.Section section = byCommand.get(command);
		if (section == null) {
			return Optional.empty();
		}
		return Optional.of(number.filter(n -> n <= Integer.MAX_VALUE)
				.map(n -> chooser.forNumber(userId, n.intValue(), section))
				.orElseGet(() -> chooser.choose(userId, section)));
	}

	/** Нажатая кнопка выбора поездки: «🏛 2. Рим · 12 мая». */
	public Optional<BotReply> choice(long userId, String text) {
		for (TripChooser.Section section : List.copyOf(byMenu.values())) {
			Optional<Integer> number = chooser.parseChoice(text, section);
			if (number.isPresent()) {
				return Optional.of(chooser.forNumber(userId, number.get(), section));
			}
		}
		return Optional.empty();
	}
}
