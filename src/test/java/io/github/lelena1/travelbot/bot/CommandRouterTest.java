package io.github.lelena1.travelbot.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.lelena1.travelbot.config.BotProperties;
import io.github.lelena1.travelbot.dialog.NewTripDialog;
import io.github.lelena1.travelbot.trip.Trip;
import io.github.lelena1.travelbot.trip.TripService;
import io.github.lelena1.travelbot.user.AccessService;
import io.github.lelena1.travelbot.user.AccessService.DenyResult;
import io.github.lelena1.travelbot.user.AppUser;
import io.github.lelena1.travelbot.user.Role;

class CommandRouterTest {

	private static final long ADMIN = 1L;
	private static final long USER = 2L;
	private static final long STRANGER = 3L;

	private AccessService access;
	private NewTripDialog dialog;
	private TripService tripService;
	private CommandRouter router;

	@BeforeEach
	void setUp() {
		access = mock(AccessService.class);
		when(access.isAllowed(ADMIN)).thenReturn(true);
		when(access.isAllowed(USER)).thenReturn(true);
		when(access.isAdmin(ADMIN)).thenReturn(true);
		dialog = mock(NewTripDialog.class);
		tripService = mock(TripService.class);
		router = new CommandRouter(access, dialog, tripService, new BotProperties("token", "travel_bot"));
	}

	private BotReply send(long userId, String text) {
		return router.handle(new IncomingMessage(userId, userId, "Лена", text));
	}

	@Test
	void strangerIsRejectedAndSeesOwnId() {
		BotReply reply = send(STRANGER, "/start");

		assertThat(reply.text()).contains("доступ закрыт").contains("3");
		assertThat(reply.showsMainMenu()).isFalse();
	}

	@Test
	void myIdWorksEvenForStranger() {
		assertThat(send(STRANGER, "/myid").text()).isEqualTo("Ваш Telegram ID: 3");
	}

	@Test
	void startGreetsByNameAndShowsMenu() {
		BotReply reply = send(USER, "/start");

		assertThat(reply.text()).startsWith("Привет, Лена!");
		assertThat(reply.showsMainMenu()).isTrue();
	}

	@Test
	void commandWithBotMentionIsRecognized() {
		assertThat(send(USER, "/start@travel_bot").text()).startsWith("Привет");
	}

	@Test
	void commandAddressedToAnotherBotIsIgnored() {
		assertThat(send(USER, "/start@other_bot").text()).startsWith("Не понимаю");
	}

	@Test
	void helpShowsAdminCommandsOnlyToAdmin() {
		assertThat(send(USER, "/help").text()).doesNotContain("/allow");
		assertThat(send(ADMIN, "/help").text()).contains("/allow");
	}

	@Test
	void menuButtonReportsStageInProgress() {
		BotReply reply = send(USER, MainMenu.FLIGHTS.label());

		assertThat(reply.text()).contains("Авиабилеты").contains("этап 3");
		assertThat(reply.showsMainMenu()).isTrue();
	}

	@Test
	void regularUserCannotAllowOthers() {
		BotReply reply = send(USER, "/allow 42");

		assertThat(reply.text()).contains("только администратору");
		verify(access, never()).allow(42L);
	}

	@Test
	void adminAllowsNewUser() {
		when(access.allow(42L)).thenReturn(true);

		assertThat(send(ADMIN, "/allow 42").text()).contains("42 получил доступ");
	}

	@Test
	void allowWithoutNumberExplainsFormat() {
		assertThat(send(ADMIN, "/allow abc").text()).contains("Укажите Telegram ID числом");
	}

	@Test
	void adminCannotDenyAnotherAdmin() {
		when(access.deny(5L)).thenReturn(DenyResult.IS_ADMIN);

		assertThat(send(ADMIN, "/deny 5").text()).contains("Нельзя");
	}

	@Test
	void usersListShowsRoles() {
		when(access.listUsers()).thenReturn(List.of(new AppUser(1, Role.ADMIN), new AppUser(2, Role.USER)));

		assertThat(send(ADMIN, "/users").text()).contains("1 — админ").contains("2 — пользователь");
	}

	@Test
	void unknownTextSuggestsHelp() {
		assertThat(send(USER, "привет").text()).contains("/help");
	}

	@Test
	void newTripButtonStartsQuestionnaire() {
		BotReply question = BotReply.text("Куда едем?");
		when(dialog.start(USER)).thenReturn(question);

		assertThat(send(USER, MainMenu.NEW_TRIP.label())).isSameAs(question);
		assertThat(send(USER, "/newtrip")).isSameAs(question);
	}

	@Test
	void plainTextGoesToActiveQuestionnaire() {
		BotReply answer = BotReply.text("Откуда?");
		when(dialog.isActive(USER)).thenReturn(true);
		when(dialog.handle(USER, "Рим")).thenReturn(answer);

		assertThat(send(USER, "Рим")).isSameAs(answer);
	}

	@Test
	void commandDuringQuestionnaireClosesItAndStillWorks() {
		when(dialog.isActive(USER)).thenReturn(true);

		String text = send(USER, "/help").text();

		assertThat(text).startsWith(CommandRouter.QUESTIONNAIRE_CLOSED).contains("Что я умею");
		verify(dialog).close(USER);
		verify(dialog, never()).handle(USER, "/help");
	}

	@Test
	void menuButtonDuringQuestionnaireClosesIt() {
		when(dialog.isActive(USER)).thenReturn(true);

		BotReply reply = send(USER, MainMenu.FLIGHTS.label());

		assertThat(reply.text()).startsWith(CommandRouter.QUESTIONNAIRE_CLOSED).contains("Авиабилеты");
		assertThat(reply.showsMainMenu()).isTrue();
		verify(dialog).close(USER);
	}

	@Test
	void cancelAndNewTripDoNotTriggerSilentClose() {
		when(dialog.isActive(USER)).thenReturn(true);
		when(dialog.cancel(USER)).thenReturn(BotReply.withMenu("отменено"));
		when(dialog.start(USER)).thenReturn(BotReply.text("Куда едем?"));

		assertThat(send(USER, "/cancel").text()).isEqualTo("отменено");
		assertThat(send(USER, MainMenu.NEW_TRIP.label()).text()).isEqualTo("Куда едем?");
		verify(dialog, never()).close(USER);
	}

	@Test
	void commandWithoutQuestionnaireHasNoNotice() {
		assertThat(send(USER, "/help").text()).doesNotContain("анкета");
		verify(dialog, never()).close(USER);
	}

	@Test
	void cancelWithoutQuestionnaireIsHarmless() {
		assertThat(send(USER, "/cancel").text()).contains("нечего отменять");
		verify(dialog, never()).cancel(USER);
	}

	@Test
	void strangerCannotStartQuestionnaire() {
		send(STRANGER, "/newtrip");

		verify(dialog, never()).start(STRANGER);
	}

	@Test
	void myTripsWhenEmptySuggestsCreatingOne() {
		when(tripService.activeTrips(USER)).thenReturn(List.of());

		assertThat(send(USER, "/mytrips").text()).contains("пока нет").contains("Новая поездка");
	}

	@Test
	void myTripsListsNumberedTrips() {
		Trip rome = Trip.draftFor(USER);
		rome.setDestination("Рим");
		Trip kazan = Trip.draftFor(USER);
		kazan.setDestination("Казань");
		when(tripService.activeTrips(USER)).thenReturn(List.of(rome, kazan));

		String text = send(USER, "/mytrips").text();

		assertThat(text).contains("2 поездки").contains("1. 📍 Рим").contains("2. 📍 Казань");
	}

	@Test
	void deleteTripByNumber() {
		Trip rome = Trip.draftFor(USER);
		rome.setDestination("Рим");
		when(tripService.deleteByNumber(USER, 1)).thenReturn(Optional.of(rome));

		assertThat(send(USER, "/deletetrip 1").text()).contains("«Рим» удалена");
	}

	@Test
	void deleteTripWithUnknownNumber() {
		when(tripService.deleteByNumber(USER, 7)).thenReturn(Optional.empty());

		assertThat(send(USER, "/deletetrip 7").text()).contains("нет");
		assertThat(send(USER, "/deletetrip").text()).contains("Укажите номер");
	}
}
