package io.github.lelena1.travelbot.bot;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.github.lelena1.travelbot.config.BotProperties;
import io.github.lelena1.travelbot.dialog.NewTripDialog;
import io.github.lelena1.travelbot.trip.Trip;
import io.github.lelena1.travelbot.trip.TripFormatter;
import io.github.lelena1.travelbot.trip.TripService;
import io.github.lelena1.travelbot.user.AccessService;
import io.github.lelena1.travelbot.user.AppUser;
import io.github.lelena1.travelbot.util.RussianPlural;

/**
 * Разбирает входящее сообщение и решает, что ответить.
 * Ничего не знает о Telegram API: получает {@link IncomingMessage}, возвращает {@link BotReply}.
 */
@Component
public class CommandRouter {

	static final String HELP = """
			Что я умею:
			/start — главное меню
			/newtrip — новая поездка
			/mytrips — мои поездки
			/deletetrip <номер> — удалить поездку
			/cancel — отменить заполнение анкеты
			/help — эта подсказка
			/myid — показать ваш Telegram ID

			Разделы меню появляются по мере разработки бота.""";

	static final String ADMIN_HELP = """

			Команды администратора:
			/allow <ID> — дать доступ пользователю
			/deny <ID> — забрать доступ
			/users — список пользователей""";

	private final AccessService access;
	private final NewTripDialog newTripDialog;
	private final TripService tripService;
	private final String botUsername;

	public CommandRouter(AccessService access, NewTripDialog newTripDialog, TripService tripService,
			BotProperties botProperties) {
		this.access = access;
		this.newTripDialog = newTripDialog;
		this.tripService = tripService;
		this.botUsername = botProperties.username() == null ? "" : botProperties.username();
	}

	public BotReply handle(IncomingMessage message) {
		String text = message.text() == null ? "" : message.text().trim();
		String command = extractCommand(text);

		// /myid доступна всем: так новый пользователь узнаёт свой ID и передаёт его владельцу бота
		if (command.equals("/myid")) {
			return BotReply.text("Ваш Telegram ID: " + message.userId());
		}

		if (!access.isAllowed(message.userId())) {
			return BotReply.text("""
					⛔ Это личный бот, доступ закрыт.
					Ваш Telegram ID: %d — передайте его владельцу бота, чтобы получить доступ."""
					.formatted(message.userId()));
		}

		long userId = message.userId();
		boolean admin = access.isAdmin(userId);

		switch (command) {
			case "/newtrip":
				return newTripDialog.start(userId);
			case "/mytrips":
				return myTrips(userId);
			case "/deletetrip":
				return deleteTrip(userId, text);
			case "/cancel":
				return newTripDialog.isActive(userId)
						? newTripDialog.cancel(userId)
						: BotReply.withMenu("Сейчас нечего отменять.");
			case "/start":
				return BotReply.withMenu("Привет, %s! 👋\nЯ помогу спланировать поездку: билеты, жильё, экскурсии, рестораны и достопримечательности.\nВыберите раздел в меню."
						.formatted(nameOrDefault(message.firstName())));
			case "/help":
				return BotReply.withMenu(admin ? HELP + ADMIN_HELP : HELP);
			case "/allow":
			case "/deny":
			case "/users":
				return admin ? handleAdmin(command, text) : BotReply.text("Эта команда доступна только администратору.");
			default:
				break;
		}

		Optional<MainMenu> menuItem = MainMenu.byLabel(text);
		if (menuItem.isPresent() && menuItem.get() == MainMenu.NEW_TRIP) {
			return newTripDialog.start(userId);
		}

		// Пользователь заполняет анкету: обычный текст — это ответ на вопрос.
		// Команды и кнопки главного меню обрабатываются как обычно, анкета при этом сохраняется.
		if (command.isEmpty() && menuItem.isEmpty() && newTripDialog.isActive(userId)) {
			return newTripDialog.handle(userId, text);
		}
		if (text.equals(NewTripDialog.CANCEL)) {
			return BotReply.withMenu("Хорошо.");
		}

		if (menuItem.isPresent()) {
			return BotReply.withMenu("Раздел «%s» ещё в разработке (этап %d). Скоро появится!"
					.formatted(menuItem.get().label(), menuItem.get().stage()));
		}

		return BotReply.withMenu("Не понимаю 🤔 Выберите раздел в меню или наберите /help.");
	}

	private BotReply myTrips(long userId) {
		List<Trip> trips = tripService.activeTrips(userId);
		if (trips.isEmpty()) {
			return BotReply.withMenu("Сохранённых поездок пока нет. Нажмите «%s», чтобы создать."
					.formatted(MainMenu.NEW_TRIP.label()));
		}
		StringBuilder text = new StringBuilder("🧳 Ваши поездки (%s):"
				.formatted(RussianPlural.format(trips.size(), "поездка", "поездки", "поездок")));
		for (int i = 0; i < trips.size(); i++) {
			text.append("\n\n").append(i + 1).append(". ").append(TripFormatter.summary(trips.get(i)));
		}
		text.append("\n\nУдалить поездку: /deletetrip <номер>");
		return BotReply.withMenu(text.toString());
	}

	private BotReply deleteTrip(long userId, String text) {
		Optional<Long> number = parseId(text);
		if (number.isEmpty() || number.get() > Integer.MAX_VALUE) {
			return BotReply.text("Укажите номер поездки из списка /mytrips, например: /deletetrip 1");
		}
		return tripService.deleteByNumber(userId, number.get().intValue())
				.map(trip -> BotReply.withMenu("🗑 Поездка «%s» удалена.".formatted(trip.getDestination())))
				.orElseGet(() -> BotReply.text("Поездки с таким номером нет. Посмотрите список: /mytrips"));
	}

	private BotReply handleAdmin(String command, String text) {
		if (command.equals("/users")) {
			String list = access.listUsers().stream()
					.map(CommandRouter::formatUser)
					.collect(Collectors.joining("\n"));
			return BotReply.text("Пользователи бота:\n" + list);
		}

		Optional<Long> targetId = parseId(text);
		if (targetId.isEmpty()) {
			return BotReply.text("Укажите Telegram ID числом, например: %s 123456789".formatted(command));
		}
		long id = targetId.get();

		if (command.equals("/allow")) {
			return BotReply.text(access.allow(id)
					? "✅ Пользователь %d получил доступ.".formatted(id)
					: "Пользователь %d уже есть в списке.".formatted(id));
		}

		return BotReply.text(switch (access.deny(id)) {
			case REMOVED -> "🚫 Доступ пользователя %d закрыт.".formatted(id);
			case NOT_FOUND -> "Пользователя %d нет в списке.".formatted(id);
			case IS_ADMIN -> "Нельзя закрыть доступ администратору.";
		});
	}

	/** "/start@travel_bot arg" → "/start". Чужие упоминания ботов (@other_bot) игнорируются. */
	String extractCommand(String text) {
		if (!text.startsWith("/")) {
			return "";
		}
		String first = text.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
		int at = first.indexOf('@');
		if (at < 0) {
			return first;
		}
		String mentioned = first.substring(at + 1);
		return mentioned.equalsIgnoreCase(botUsername) ? first.substring(0, at) : "";
	}

	private static Optional<Long> parseId(String text) {
		String[] parts = text.split("\\s+");
		if (parts.length < 2) {
			return Optional.empty();
		}
		try {
			return Optional.of(Long.parseLong(parts[1]));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	private static String formatUser(AppUser user) {
		return "• %d — %s".formatted(user.getTelegramId(), user.isAdmin() ? "админ" : "пользователь");
	}

	private static String nameOrDefault(String firstName) {
		return firstName == null || firstName.isBlank() ? "путешественник" : firstName;
	}
}
