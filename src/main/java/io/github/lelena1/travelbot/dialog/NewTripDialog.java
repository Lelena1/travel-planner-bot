package io.github.lelena1.travelbot.dialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lelena1.travelbot.bot.BotReply;
import io.github.lelena1.travelbot.trip.Budget;
import io.github.lelena1.travelbot.trip.Pets;
import io.github.lelena1.travelbot.trip.Trip;
import io.github.lelena1.travelbot.trip.TripFormatter;
import io.github.lelena1.travelbot.trip.TripRepository;

/**
 * Анкета «Новая поездка» — конечный автомат:
 * куда → откуда → даты → взрослые → дети → (возраст детей) → питомцы → бюджет → подтверждение.
 * <p>
 * Ответы сразу сохраняются в черновик поездки, поэтому при перезапуске бота анкета не теряется.
 */
@Service
public class NewTripDialog {

	public static final String CANCEL = "❌ Отмена";
	static final String SAVE = "✅ Сохранить";
	static final String RESTART = "🔄 Заново";
	static final String NO_CHILDREN = "Нет";

	static final int MAX_ADULTS = 20;
	static final int MAX_CHILDREN = 10;
	static final int MAX_CHILD_AGE = 17;

	private static final List<String> CANCEL_ROW = List.of(CANCEL);

	private final ConversationRepository conversations;
	private final TripRepository trips;
	private final DateRangeParser dateParser;

	public NewTripDialog(ConversationRepository conversations, TripRepository trips, DateRangeParser dateParser) {
		this.conversations = conversations;
		this.trips = trips;
		this.dateParser = dateParser;
	}

	@Transactional(readOnly = true)
	public boolean isActive(long userId) {
		return conversations.existsById(userId);
	}

	/** Начинает анкету заново; незаконченный черновик удаляется. */
	@Transactional
	public BotReply start(long userId) {
		discardDraft(userId);
		Trip draft = trips.save(Trip.draftFor(userId));
		conversations.save(new Conversation(userId, draft.getId()));
		return ask(DialogStep.DESTINATION, draft);
	}

	@Transactional
	public BotReply cancel(long userId) {
		discardDraft(userId);
		return BotReply.withMenu("Хорошо, создание поездки отменено.");
	}

	@Transactional
	public BotReply handle(long userId, String rawText) {
		String text = rawText == null ? "" : rawText.trim();
		if (text.equals(CANCEL)) {
			return cancel(userId);
		}
		Optional<Conversation> found = conversations.findById(userId);
		if (found.isEmpty()) {
			return BotReply.withMenu("Анкета не найдена. Нажмите «🗺 Новая поездка», чтобы начать.");
		}
		Conversation conversation = found.get();
		Trip trip = trips.findById(conversation.getTripId()).orElseThrow();

		return switch (conversation.getStep()) {
			case DESTINATION -> onDestination(conversation, trip, text);
			case ORIGIN -> onOrigin(conversation, trip, text);
			case DATES -> onDates(conversation, trip, text);
			case ADULTS -> onAdults(conversation, trip, text);
			case CHILDREN -> onChildren(conversation, trip, text);
			case CHILD_AGES -> onChildAges(conversation, trip, text);
			case PETS -> onPets(conversation, trip, text);
			case BUDGET -> onBudget(conversation, trip, text);
			case CONFIRM -> onConfirm(conversation, trip, text);
		};
	}

	// ---------- обработчики шагов ----------

	private BotReply onDestination(Conversation conversation, Trip trip, String text) {
		Optional<String> place = validPlace(text);
		if (place.isEmpty()) {
			return retry("Напишите страну и/или город, например: Италия, Рим", DialogStep.DESTINATION, trip);
		}
		trip.setDestination(place.get());
		return next(conversation, DialogStep.ORIGIN, trip);
	}

	private BotReply onOrigin(Conversation conversation, Trip trip, String text) {
		Optional<String> place = validPlace(text);
		if (place.isEmpty()) {
			return retry("Напишите город, откуда отправляетесь.", DialogStep.ORIGIN, trip);
		}
		trip.setOrigin(place.get());
		return next(conversation, DialogStep.DATES, trip);
	}

	private BotReply onDates(Conversation conversation, Trip trip, String text) {
		DateRangeParser.Result result = dateParser.parse(text);
		if (result instanceof DateRangeParser.Failure failure) {
			return retry(failure.message(), DialogStep.DATES, trip);
		}
		DateRangeParser.Success dates = (DateRangeParser.Success) result;
		trip.setDates(dates.from(), dates.to());
		return next(conversation, DialogStep.ADULTS, trip);
	}

	private BotReply onAdults(Conversation conversation, Trip trip, String text) {
		Optional<Integer> adults = parseNumber(text, 1, MAX_ADULTS);
		if (adults.isEmpty()) {
			return retry("Нужно число от 1 до %d.".formatted(MAX_ADULTS), DialogStep.ADULTS, trip);
		}
		trip.setAdults(adults.get());
		return next(conversation, DialogStep.CHILDREN, trip);
	}

	private BotReply onChildren(Conversation conversation, Trip trip, String text) {
		Optional<Integer> children = text.equalsIgnoreCase(NO_CHILDREN)
				? Optional.of(0)
				: parseNumber(text, 0, MAX_CHILDREN);
		if (children.isEmpty()) {
			return retry("Нужно число от 0 до %d.".formatted(MAX_CHILDREN), DialogStep.CHILDREN, trip);
		}
		trip.setChildren(children.get());
		trip.setChildAges(List.of());
		return next(conversation, children.get() > 0 ? DialogStep.CHILD_AGES : DialogStep.PETS, trip);
	}

	private BotReply onChildAges(Conversation conversation, Trip trip, String text) {
		Optional<List<Integer>> ages = parseAges(text, trip.getChildren());
		if (ages.isEmpty()) {
			String example = trip.getChildren() == 1 ? "например: 7" : "через запятую, например: 5, 8";
			return retry("Укажите возраст %s (от 0 до %d), %s"
					.formatted(trip.getChildren() == 1 ? "ребёнка" : "каждого ребёнка", MAX_CHILD_AGE, example),
					DialogStep.CHILD_AGES, trip);
		}
		trip.setChildAges(ages.get());
		return next(conversation, DialogStep.PETS, trip);
	}

	private BotReply onPets(Conversation conversation, Trip trip, String text) {
		Optional<Pets> pets = Pets.byLabel(text);
		if (pets.isEmpty()) {
			return retry("Выберите вариант кнопкой.", DialogStep.PETS, trip);
		}
		trip.setPets(pets.get());
		return next(conversation, DialogStep.BUDGET, trip);
	}

	private BotReply onBudget(Conversation conversation, Trip trip, String text) {
		Optional<Budget> budget = Budget.byLabel(text);
		if (budget.isEmpty()) {
			return retry("Выберите вариант кнопкой.", DialogStep.BUDGET, trip);
		}
		trip.setBudget(budget.get());
		return next(conversation, DialogStep.CONFIRM, trip);
	}

	private BotReply onConfirm(Conversation conversation, Trip trip, String text) {
		if (text.equals(RESTART)) {
			return start(conversation.getUserId());
		}
		if (!text.equals(SAVE)) {
			return retry("Нажмите «%s» или «%s».".formatted(SAVE, RESTART), DialogStep.CONFIRM, trip);
		}
		trip.activate();
		conversations.delete(conversation);
		return BotReply.withMenu("""
				🎉 Поездка сохранена!

				%s

				Все поездки — команда /mytrips."""
				.formatted(TripFormatter.summary(trip)));
	}

	// ---------- вопросы ----------

	private BotReply next(Conversation conversation, DialogStep step, Trip trip) {
		conversation.moveTo(step);
		return ask(step, trip);
	}

	private BotReply retry(String hint, DialogStep step, Trip trip) {
		BotReply question = ask(step, trip);
		return new BotReply("⚠️ " + hint + "\n\n" + question.text(), question.keyboard());
	}

	BotReply ask(DialogStep step, Trip trip) {
		return switch (step) {
			case DESTINATION -> BotReply.withOptions(
					"🗺 Куда едем? Напишите страну и/или город.\nНапример: Италия, Рим",
					List.of(CANCEL_ROW));
			case ORIGIN -> BotReply.withOptions(
					"🛫 Откуда отправляетесь? Выберите город или напишите свой.",
					List.of(List.of("Москва", "Санкт-Петербург"), CANCEL_ROW));
			case DATES -> BotReply.withOptions(
					"🗓 Когда? Напишите даты поездки.\nНапример: 12.05–18.05 или 12–18 мая",
					List.of(CANCEL_ROW));
			case ADULTS -> BotReply.withOptions(
					"👥 Сколько взрослых едет?",
					List.of(List.of("1", "2", "3", "4"), List.of("5", "6"), CANCEL_ROW));
			case CHILDREN -> BotReply.withOptions(
					"🧒 А детей?",
					List.of(List.of(NO_CHILDREN, "1", "2", "3"), CANCEL_ROW));
			case CHILD_AGES -> BotReply.withOptions(trip.getChildren() == 1
					? "Сколько лет ребёнку?"
					: "Сколько лет детям? Напишите через запятую, например: 5, 8",
					List.of(CANCEL_ROW));
			case PETS -> BotReply.withOptions(
					"🐾 Берёте с собой питомца? Это важно при поиске жилья.",
					List.of(labels(Pets.values()).subList(0, 2), labels(Pets.values()).subList(2, 4), CANCEL_ROW));
			case BUDGET -> BotReply.withOptions(
					"💰 Бюджет на жильё за ночь?",
					List.of(labels(Budget.values()).subList(0, 2), labels(Budget.values()).subList(2, 4), CANCEL_ROW));
			case CONFIRM -> BotReply.withOptions(
					"Проверьте, всё ли верно:\n\n" + TripFormatter.summary(trip),
					List.of(List.of(SAVE, RESTART), CANCEL_ROW));
		};
	}

	// ---------- разбор ответов ----------

	private static Optional<String> validPlace(String text) {
		String place = text.replaceAll("\\s+", " ").trim();
		boolean hasLetter = place.codePoints().anyMatch(Character::isLetter);
		if (place.length() < 2 || place.length() > 100 || !hasLetter || place.startsWith("/")) {
			return Optional.empty();
		}
		return Optional.of(place);
	}

	private static Optional<Integer> parseNumber(String text, int min, int max) {
		try {
			int value = Integer.parseInt(text.trim());
			return value >= min && value <= max ? Optional.of(value) : Optional.empty();
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	private static Optional<List<Integer>> parseAges(String text, int expectedCount) {
		String[] parts = text.split("[,;\\s]+");
		List<Integer> ages = new ArrayList<>();
		for (String part : parts) {
			if (part.isBlank()) {
				continue;
			}
			Optional<Integer> age = parseNumber(part, 0, MAX_CHILD_AGE);
			if (age.isEmpty()) {
				return Optional.empty();
			}
			ages.add(age.get());
		}
		return ages.size() == expectedCount ? Optional.of(ages) : Optional.empty();
	}

	private static List<String> labels(Pets[] values) {
		return Arrays.stream(values).map(Pets::label).toList();
	}

	private static List<String> labels(Budget[] values) {
		return Arrays.stream(values).map(Budget::label).toList();
	}

	private void discardDraft(long userId) {
		conversations.findById(userId).ifPresent(conversation -> {
			conversations.delete(conversation);
			trips.deleteById(conversation.getTripId());
			// сразу выполняем удаление, чтобы в той же транзакции можно было создать новую анкету с тем же user_id
			conversations.flush();
		});
	}
}
