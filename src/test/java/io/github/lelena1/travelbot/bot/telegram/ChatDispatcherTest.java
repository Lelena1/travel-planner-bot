package io.github.lelena1.travelbot.bot.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class ChatDispatcherTest {

	@Test
	void messagesOfOneChatKeepTheirOrder() throws Exception {
		List<Integer> handled = new CopyOnWriteArrayList<>();
		try (ChatDispatcher dispatcher = new ChatDispatcher(4)) {
			CompletableFuture<Void> last = null;
			for (int i = 1; i <= 20; i++) {
				int number = i;
				last = dispatcher.submit(42, () -> {
					sleep(number % 3);   // разная длительность не должна менять порядок
					handled.add(number);
				});
			}
			last.get(5, TimeUnit.SECONDS);
		}
		assertThat(handled).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20);
	}

	@Test
	void slowChatDoesNotBlockOtherChats() throws Exception {
		CountDownLatch slowCanFinish = new CountDownLatch(1);
		CountDownLatch fastDone = new CountDownLatch(1);
		try (ChatDispatcher dispatcher = new ChatDispatcher(4)) {
			dispatcher.submit(1, () -> await(slowCanFinish));   // «ищет рестораны» долго
			dispatcher.submit(2, fastDone::countDown);           // другой пользователь

			assertThat(fastDone.await(2, TimeUnit.SECONDS)).as("второй чат обслужен, пока первый ждёт").isTrue();
			slowCanFinish.countDown();
		}
	}

	@Test
	void failingTaskDoesNotBreakTheChatQueue() throws Exception {
		List<String> handled = new CopyOnWriteArrayList<>();
		try (ChatDispatcher dispatcher = new ChatDispatcher(2)) {
			dispatcher.submit(7, () -> {
				throw new IllegalStateException("сбой");
			});
			dispatcher.submit(7, () -> handled.add("после сбоя")).get(2, TimeUnit.SECONDS);
		}
		assertThat(handled).containsExactly("после сбоя");
	}

	@Test
	void finishedChatsAreForgotten() throws Exception {
		try (ChatDispatcher dispatcher = new ChatDispatcher(2)) {
			dispatcher.submit(1, () -> { }).get(2, TimeUnit.SECONDS);
			Thread.sleep(50);
			assertThat(dispatcher.activeChats()).isZero();
		}
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static void await(CountDownLatch latch) {
		try {
			latch.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
