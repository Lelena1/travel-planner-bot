package io.github.lelena1.travelbot.bot.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import io.github.lelena1.travelbot.bot.BotReply;
import io.github.lelena1.travelbot.bot.Keyboard;

class TelegramSenderTest {

	@Test
	void keepDoesNotSendAnyMarkup() {
		assertThat(TelegramSender.toMarkup(Keyboard.KEEP)).isNull();
	}

	@Test
	void removeHidesKeyboard() {
		assertThat(TelegramSender.toMarkup(Keyboard.REMOVE)).isInstanceOf(ReplyKeyboardRemove.class);
	}

	@Test
	void mainMenuHasAllSectionsInThreeRows() {
		ReplyKeyboardMarkup markup = (ReplyKeyboardMarkup) TelegramSender.toMarkup(Keyboard.MAIN_MENU);

		assertThat(markup.getKeyboard()).hasSize(3);
		assertThat(markup.getKeyboard().get(0)).extracting(KeyboardButton::getText)
				.containsExactly("🗺 Новая поездка", "✈️ Авиабилеты", "🔔 Мои подписки");
	}

	@Test
	void optionsAreShownAsRowsOfButtons() {
		Keyboard options = Keyboard.options(List.of(List.of("1", "2", "3"), List.of("❌ Отмена")));

		ReplyKeyboardMarkup markup = (ReplyKeyboardMarkup) TelegramSender.toMarkup(options);

		assertThat(markup.getKeyboard()).hasSize(2);
		assertThat(markup.getKeyboard().get(0)).extracting(KeyboardButton::getText).containsExactly("1", "2", "3");
		assertThat(markup.getKeyboard().get(1)).extracting(KeyboardButton::getText).containsExactly("❌ Отмена");
		assertThat(markup.getOneTimeKeyboard()).isTrue();
	}

	@Test
	void linksBecomeInlineUrlButtons() {
		Keyboard links = Keyboard.links(List.of(
				List.of(new Keyboard.Link("Купить", "https://www.aviasales.ru/search/MOW1205ROM18051"))));

		InlineKeyboardMarkup markup = (InlineKeyboardMarkup) TelegramSender.toMarkup(links);

		InlineKeyboardButton button = markup.getKeyboard().get(0).get(0);
		assertThat(button.getText()).isEqualTo("Купить");
		assertThat(button.getUrl()).isEqualTo("https://www.aviasales.ru/search/MOW1205ROM18051");
	}

	@Test
	void actionButtonSendsCallbackData() {
		Keyboard links = Keyboard.links(List.of(List.of(Keyboard.Link.action("🔔 Следить за ценой", "watch:12"))));

		InlineKeyboardButton button = ((InlineKeyboardMarkup) TelegramSender.toMarkup(links))
				.getKeyboard().get(0).get(0);

		assertThat(button.getCallbackData()).isEqualTo("watch:12");
		assertThat(button.getUrl()).isNull();
	}

	@Test
	void buttonMustBeEitherLinkOrAction() {
		assertThatThrownBy(() -> new Keyboard.Link("x", "https://a", "b"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Keyboard.Link.action("x", "a".repeat(65)))
				.hasMessageContaining("64");
	}

	@Test
	void sendReportsDeliveryResult() throws Exception {
		TelegramClient client = mock(TelegramClient.class);
		TelegramSender sender = new TelegramSender(client);

		assertThat(sender.send(42, BotReply.text("Цена упала"))).isTrue();

		ArgumentCaptor<SendMessage> sent = ArgumentCaptor.forClass(SendMessage.class);
		verify(client).execute(sent.capture());
		assertThat(sent.getValue().getChatId()).isEqualTo("42");
		assertThat(sent.getValue().getText()).isEqualTo("Цена упала");

		when(client.execute(any(SendMessage.class))).thenThrow(new TelegramApiException("blocked"));
		assertThat(sender.send(42, BotReply.text("x"))).isFalse();
	}
}
