package io.github.lelena1.travelbot.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.lelena1.travelbot.config.BotProperties;
import io.github.lelena1.travelbot.user.AccessService;
import io.github.lelena1.travelbot.user.AccessService.DenyResult;
import io.github.lelena1.travelbot.user.AppUser;
import io.github.lelena1.travelbot.user.Role;

class CommandRouterTest {

	private static final long ADMIN = 1L;
	private static final long USER = 2L;
	private static final long STRANGER = 3L;

	private AccessService access;
	private CommandRouter router;

	@BeforeEach
	void setUp() {
		access = mock(AccessService.class);
		when(access.isAllowed(ADMIN)).thenReturn(true);
		when(access.isAllowed(USER)).thenReturn(true);
		when(access.isAdmin(ADMIN)).thenReturn(true);
		router = new CommandRouter(access, new BotProperties("token", "travel_bot"));
	}

	private BotReply send(long userId, String text) {
		return router.handle(new IncomingMessage(userId, userId, "Лена", text));
	}

	@Test
	void strangerIsRejectedAndSeesOwnId() {
		BotReply reply = send(STRANGER, "/start");

		assertThat(reply.text()).contains("доступ закрыт").contains("3");
		assertThat(reply.showMainMenu()).isFalse();
	}

	@Test
	void myIdWorksEvenForStranger() {
		assertThat(send(STRANGER, "/myid").text()).isEqualTo("Ваш Telegram ID: 3");
	}

	@Test
	void startGreetsByNameAndShowsMenu() {
		BotReply reply = send(USER, "/start");

		assertThat(reply.text()).startsWith("Привет, Лена!");
		assertThat(reply.showMainMenu()).isTrue();
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
		assertThat(reply.showMainMenu()).isTrue();
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
}
