package io.github.lelena1.travelbot.bot.telegram;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Выполняет задачи разных чатов параллельно, а задачи одного чата — строго по очереди.
 * <p>
 * Медленный запрос (например, к OpenStreetMap) не должен останавливать бота для всех.
 * Но в одном чате порядок важен: ответы на вопросы анкеты должны обрабатываться в том порядке,
 * в каком их отправили.
 */
public class ChatDispatcher implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(ChatDispatcher.class);

	private final ExecutorService pool;
	/** Последняя задача каждого чата: следующая задача чата запускается после неё. */
	private final ConcurrentHashMap<Long, CompletableFuture<Void>> tails = new ConcurrentHashMap<>();

	public ChatDispatcher(int threads) {
		this.pool = Executors.newFixedThreadPool(threads);
	}

	public CompletableFuture<Void> submit(long chatId, Runnable task) {
		CompletableFuture<Void> next = tails.compute(chatId, (id, previous) ->
				(previous == null ? CompletableFuture.<Void>completedFuture(null) : previous)
						.thenRunAsync(safely(chatId, task), pool));
		// когда очередь чата опустела, забываем его — карта не растёт бесконечно
		next.whenComplete((result, error) -> tails.remove(chatId, next));
		return next;
	}

	private static Runnable safely(long chatId, Runnable task) {
		return () -> {
			try {
				task.run();
			} catch (RuntimeException e) {
				log.error("Ошибка при обработке сообщения в чате {}", chatId, e);
			}
		};
	}

	int activeChats() {
		return tails.size();
	}

	@Override
	public void close() throws InterruptedException {
		pool.shutdown();
		pool.awaitTermination(10, TimeUnit.SECONDS);
	}
}
