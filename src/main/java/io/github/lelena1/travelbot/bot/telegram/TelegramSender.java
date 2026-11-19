package io.github.lelena1.travelbot.bot.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import io.github.lelena1.travelbot.bot.BotReply;
import io.github.lelena1.travelbot.bot.Keyboard;
import io.github.lelena1.travelbot.bot.MainMenu;
import io.github.lelena1.travelbot.bot.Notifier;

/** Отправляет {@link BotReply} в Telegram: и ответы на сообщения, и уведомления. */
public class TelegramSender implements Notifier {

	private static final Logger log = LoggerFactory.getLogger(TelegramSender.class);

	private static final ReplyKeyboardMarkup MAIN_MENU = ReplyKeyboardMarkup.builder()
			.keyboard(MainMenu.LAYOUT.stream()
					.map(row -> new KeyboardRow(row.stream().map(MainMenu::label).toArray(String[]::new)))
					.toList())
			.resizeKeyboard(true)
			.build();

	private final TelegramClient client;

	public TelegramSender(TelegramClient client) {
		this.client = client;
	}

	@Override
	public boolean send(long chatId, BotReply reply) {
		SendMessage.SendMessageBuilder<?, ?> builder = SendMessage.builder()
				.chatId(chatId)
				.text(reply.text());
		ReplyKeyboard markup = toMarkup(reply.keyboard());
		if (markup != null) {
			builder.replyMarkup(markup);
		}
		try {
			client.execute(builder.build());
			return true;
		} catch (TelegramApiException e) {
			log.error("Не удалось отправить сообщение в чат {}", chatId, e);
			return false;
		}
	}

	/** Превращает описание кнопок в объект Telegram API; null — клавиатуру не менять. */
	static ReplyKeyboard toMarkup(Keyboard keyboard) {
		if (keyboard instanceof Keyboard.MainMenuKeyboard) {
			return MAIN_MENU;
		}
		if (keyboard instanceof Keyboard.Remove) {
			return ReplyKeyboardRemove.builder().removeKeyboard(true).build();
		}
		if (keyboard instanceof Keyboard.Options options) {
			return ReplyKeyboardMarkup.builder()
					.keyboard(options.rows().stream()
							.map(row -> new KeyboardRow(row.toArray(String[]::new)))
							.toList())
					.resizeKeyboard(true)
					.oneTimeKeyboard(true)
					.build();
		}
		if (keyboard instanceof Keyboard.Links links) {
			return InlineKeyboardMarkup.builder()
					.keyboard(links.rows().stream()
							.map(row -> new InlineKeyboardRow(row.stream()
									.map(TelegramSender::inlineButton)
									.toList()))
							.toList())
					.build();
		}
		return null; // Keyboard.Keep
	}

	private static InlineKeyboardButton inlineButton(Keyboard.Link link) {
		InlineKeyboardButton.InlineKeyboardButtonBuilder<?, ?> button = InlineKeyboardButton.builder().text(link.text());
		return link.isAction() ? button.callbackData(link.callbackData()).build() : button.url(link.url()).build();
	}
}
