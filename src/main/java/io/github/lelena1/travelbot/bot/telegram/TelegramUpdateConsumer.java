package io.github.lelena1.travelbot.bot.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import io.github.lelena1.travelbot.bot.BotReply;
import io.github.lelena1.travelbot.bot.CommandRouter;
import io.github.lelena1.travelbot.bot.IncomingMessage;

/**
 * Адаптер между Telegram и логикой бота: превращает Update в {@link IncomingMessage},
 * а ответ отправляет через {@link TelegramSender}.
 */
public class TelegramUpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

	private static final Logger log = LoggerFactory.getLogger(TelegramUpdateConsumer.class);

	private final TelegramClient client;
	private final TelegramSender sender;
	private final CommandRouter router;
	private final ChatDispatcher dispatcher;

	public TelegramUpdateConsumer(TelegramClient client, TelegramSender sender, CommandRouter router,
			ChatDispatcher dispatcher) {
		this.client = client;
		this.sender = sender;
		this.router = router;
		this.dispatcher = dispatcher;
	}

	/**
	 * Поток опроса Telegram только раскладывает сообщения по очередям чатов и сразу берёт следующие:
	 * медленный ответ одному пользователю не задерживает остальных.
	 */
	@Override
	public void consume(Update update) {
		Long chatId = chatId(update);
		if (chatId != null) {
			dispatcher.submit(chatId, () -> process(update));
		}
	}

	static Long chatId(Update update) {
		if (update.hasCallbackQuery() && update.getCallbackQuery().getMessage() != null) {
			return update.getCallbackQuery().getMessage().getChatId();
		}
		if (update.hasMessage()) {
			return update.getMessage().getChatId();
		}
		return null;
	}

	void process(Update update) {
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
			sender.send(incoming.chatId(), router.handle(incoming));
		} catch (RuntimeException e) {
			log.error("Ошибка при обработке сообщения от {}", incoming.userId(), e);
			sender.send(incoming.chatId(), BotReply.text("😔 Что-то пошло не так. Попробуйте ещё раз чуть позже."));
		}
	}
}
