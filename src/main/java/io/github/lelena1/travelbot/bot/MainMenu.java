package io.github.lelena1.travelbot.bot;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Кнопки главного меню. Разделы появятся по мере реализации этапов. */
public enum MainMenu {

	NEW_TRIP("🗺 Новая поездка", 2),
	FLIGHTS("✈️ Авиабилеты", 3),
	SUBSCRIPTIONS("🔔 Мои подписки", 4),
	LODGING("🏨 Жильё", 5),
	TOURS("🎟 Экскурсии", 6),
	PLACES("🏛 Места", 6),
	RESTAURANTS("🍽 Рестораны", 6),
	ITINERARY("🗓 План", 7);

	/** Расположение кнопок по рядам. */
	public static final List<List<MainMenu>> LAYOUT = List.of(
			List.of(NEW_TRIP, FLIGHTS, SUBSCRIPTIONS),
			List.of(LODGING, TOURS, PLACES),
			List.of(RESTAURANTS, ITINERARY));

	private final String label;
	private final int stage;

	MainMenu(String label, int stage) {
		this.label = label;
		this.stage = stage;
	}

	public String label() {
		return label;
	}

	/** Номер этапа разработки, на котором появится раздел. */
	public int stage() {
		return stage;
	}

	public static Optional<MainMenu> byLabel(String text) {
		return Arrays.stream(values()).filter(item -> item.label.equals(text)).findFirst();
	}
}
