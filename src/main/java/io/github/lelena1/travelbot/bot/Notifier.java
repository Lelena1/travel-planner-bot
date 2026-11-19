package io.github.lelena1.travelbot.bot;

/**
 * Отправка сообщения по инициативе бота (например, «цена упала»), а не в ответ пользователю.
 * Интерфейс отделяет логику от Telegram: в тестах подставляется простая заглушка.
 */
public interface Notifier {

	/** @return true, если сообщение доставлено */
	boolean send(long chatId, BotReply message);
}
