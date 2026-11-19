package io.github.lelena1.travelbot.bot;

/**
 * Входящее сообщение, независимое от библиотеки Telegram.
 * Благодаря этому логику бота можно тестировать без подключения к Telegram.
 */
public record IncomingMessage(long userId, long chatId, String firstName, String text) {
}
