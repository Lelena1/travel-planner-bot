package io.github.lelena1.travelbot.bot.telegram;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import io.github.lelena1.travelbot.bot.BotReply;
import io.github.lelena1.travelbot.bot.CommandRouter;
import io.github.lelena1.travelbot.bot.IncomingMessage;
import io.github.lelena1.travelbot.bot.MainMenu;

/**
 * Адаптер между Telegram и логикой бота: превращает Update в {@link IncomingMessage}
 * и отправляет {@link BotReply} обратно в чат.
 */
public class TelegramUpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

	private static final Logger log = LoggerFactory.getLogger(TelegramUpdateConsumer.class);

	private static final ReplyKeyboardMarkup MAIN_MENU = ReplyKeyboardMarkup.builder()
			.keyboard(MainMenu.LAYOUT.stream()
					.map(row -> new KeyboardRow(row.stream().map(MainMenu::label).toArray(String[]::new)))
					.toList())
			.resizeKeyboard(true)
			.build();

	private final TelegramClient client;
	private final CommandRouter router;

	public TelegramUpdateConsumer(TelegramClient client, CommandRouter router) {
		this.client = client;
		this.router = router;
	}

	@Override
	public void consume(Update update) {
		if (!update.hasMessage() || !update.getMessage().hasText()) {
			return;
		}
		Message message = update.getMessage();
		IncomingMessage incoming = new IncomingMessage(
				message.getFrom().getId(),
				message.getChatId(),
				message.getFrom().getFirstName(),
				message.getText());

		try {
			send(incoming.chatId(), router.handle(incoming));
		} catch (RuntimeException e) {
			log.error("Ошибка при обработке сообщения от {}", incoming.userId(), e);
			send(incoming.chatId(), BotReply.text("😔 Что-то пошло не так. Попробуйте ещё раз чуть позже."));
		}
	}

	private void send(long chatId, BotReply reply) {
		SendMessage.SendMessageBuilder<?, ?> builder = SendMessage.builder()
				.chatId(chatId)
				.text(reply.text());
		if (reply.showMainMenu()) {
			builder.replyMarkup(MAIN_MENU);
		}
		try {
			client.execute(builder.build());
		} catch (TelegramApiException e) {
			log.error("Не удалось отправить сообщение в чат {}", chatId, e);
		}
	}

	/** Для тестов: какие кнопки увидит пользователь. */
	static List<KeyboardRow> mainMenuRows() {
		return MAIN_MENU.getKeyboard();
	}
}
