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
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
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
				.text(fitToTelegram(reply.text()));
		ReplyKeyboard markup = toMarkup(reply.keyboard());
		if (markup != null) {
			builder.replyMarkup(markup);
		}
		SendMessage message = builder.build();
		for (int attempt = 1; ; attempt++) {
			try {
				client.execute(message);
				return true;
			} catch (TelegramApiException e) {
				if (!isNetworkError(e) || attempt >= MAX_ATTEMPTS) {
					log.error("Не удалось отправить сообщение в чат {} (попытка {})", chatId, attempt, e);
					return false;
				}
				log.warn("Сбой сети при отправке в чат {}, повторяю (попытка {}): {}", chatId, attempt, e.getMessage());
				pause(RETRY_PAUSE_MILLIS * attempt);
			}
		}
	}

	/** Telegram не принимает сообщения длиннее 4096 символов — лучше обрезать, чем потерять ответ. */
	static final int MAX_MESSAGE_LENGTH = 4096;

	static String fitToTelegram(String text) {
		if (text.length() <= MAX_MESSAGE_LENGTH) {
			return text;
		}
		log.warn("Сообщение длиной {} символов обрезано до {}", text.length(), MAX_MESSAGE_LENGTH);
		int cut = text.lastIndexOf('\n', MAX_MESSAGE_LENGTH - 2);
		return text.substring(0, cut > 0 ? cut : MAX_MESSAGE_LENGTH - 1) + "\n…";
	}

	static final int MAX_ATTEMPTS = 3;
	static final long RETRY_PAUSE_MILLIS = 1_000;

	/**
	 * Повторять имеет смысл только сбой сети (тайм-аут, обрыв соединения). Ошибку от самого Telegram
	 * (например, 400 — неверная кнопка) повтор не исправит.
	 */
	static boolean isNetworkError(TelegramApiException e) {
		if (e instanceof TelegramApiRequestException) {
			return false;
		}
		for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
			if (cause instanceof java.io.IOException) {
				return true;
			}
		}
		return false;
	}

	private static void pause(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
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
