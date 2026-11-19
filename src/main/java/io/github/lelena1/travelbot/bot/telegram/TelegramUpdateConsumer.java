package io.github.lelena1.travelbot.bot.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
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
import io.github.lelena1.travelbot.bot.CommandRouter;
import io.github.lelena1.travelbot.bot.IncomingMessage;
import io.github.lelena1.travelbot.bot.Keyboard;
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
		if (update.hasCallbackQuery()) {
			consumeButton(update.getCallbackQuery());
			return;
		}
		if (!update.hasMessage() || !update.getMessage().hasText()) {
			return;
		}
		Message message = update.getMessage();
		IncomingMessage incoming = new IncomingMessage(
				message.getFrom().getId(),
				message.getChatId(),
				message.getFrom().getFirstName(),
				message.getText());

		if (incoming.text().startsWith("/")) {
			// записываем только саму команду, без аргументов и без обычного текста пользователя
			log.info("Команда {} от пользователя {}", incoming.text().split("\\s+", 2)[0], incoming.userId());
		}
		handle(incoming);
	}

	/**
	 * Нажатие кнопки под сообщением (callback). Данные кнопки обрабатываются как текст,
	 * а Telegram получает подтверждение, чтобы у пользователя пропали «часики» на кнопке.
	 */
	private void consumeButton(CallbackQuery callback) {
		try {
			client.execute(AnswerCallbackQuery.builder().callbackQueryId(callback.getId()).build());
		} catch (TelegramApiException e) {
			log.warn("Не удалось подтвердить нажатие кнопки", e);
		}
		if (callback.getData() == null || callback.getMessage() == null) {
			return;
		}
		log.info("Кнопка {} от пользователя {}", callback.getData().split(":", 2)[0], callback.getFrom().getId());
		handle(new IncomingMessage(
				callback.getFrom().getId(),
				callback.getMessage().getChatId(),
				callback.getFrom().getFirstName(),
				callback.getData()));
	}

	private void handle(IncomingMessage incoming) {
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
		ReplyKeyboard markup = toMarkup(reply.keyboard());
		if (markup != null) {
			builder.replyMarkup(markup);
		}
		try {
			client.execute(builder.build());
		} catch (TelegramApiException e) {
			log.error("Не удалось отправить сообщение в чат {}", chatId, e);
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
									.map(TelegramUpdateConsumer::inlineButton)
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
