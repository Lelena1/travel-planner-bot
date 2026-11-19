package io.github.lelena1.travelbot.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.lelena1.travelbot.TestcontainersConfiguration;
import io.github.lelena1.travelbot.bot.BotReply;
import io.github.lelena1.travelbot.bot.Keyboard;
import io.github.lelena1.travelbot.trip.Budget;
import io.github.lelena1.travelbot.trip.Pets;
import io.github.lelena1.travelbot.trip.Trip;
import io.github.lelena1.travelbot.trip.TripRepository;
import io.github.lelena1.travelbot.trip.TripStatus;
import io.github.lelena1.travelbot.user.AccessService;

/** Проходит анкету целиком на настоящей PostgreSQL. Без Docker тест пропускается. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@Import({ TestcontainersConfiguration.class, NewTripDialogIntegrationTest.FixedClock.class })
class NewTripDialogIntegrationTest {

	private static final AtomicLong NEXT_USER = new AtomicLong(10_000);

	@TestConfiguration
	static class FixedClock {
		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(LocalDate.of(2026, 11, 19).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
		}
	}

	@Autowired
	private NewTripDialog dialog;
	@Autowired
	private TripRepository trips;
	@Autowired
	private ConversationRepository conversations;
	@Autowired
	private AccessService access;

	private long user;

	@BeforeEach
	void newUser() {
		user = NEXT_USER.incrementAndGet();
		access.allow(user);
	}

	@Test
	void fullQuestionnaireWithChildrenAndDog() {
		assertThat(dialog.start(user).text()).contains("Куда едем");
		assertThat(dialog.handle(user, "Италия, Рим").text()).contains("Откуда");
		assertThat(dialog.handle(user, "Москва").text()).contains("Когда");
		assertThat(dialog.handle(user, "12-18 мая").text()).contains("взрослых");
		assertThat(dialog.handle(user, "2").text()).contains("детей");
		assertThat(dialog.handle(user, "2").text()).contains("Сколько лет детям");
		assertThat(dialog.handle(user, "5, 8").text()).contains("питомца");
		assertThat(dialog.handle(user, Pets.DOG.label()).text()).contains("Бюджет");

		BotReply confirm = dialog.handle(user, Budget.UP_TO_6000.label());
		assertThat(confirm.text()).contains("Проверьте").contains("Италия, Рим").contains("6 ночей");

		BotReply saved = dialog.handle(user, NewTripDialog.SAVE);
		assertThat(saved.text()).contains("Поездка сохранена");
		assertThat(saved.showsMainMenu()).isTrue();

		List<Trip> active = trips.findByUserIdAndStatusOrderByDateFromAsc(user, TripStatus.ACTIVE);
		assertThat(active).singleElement().satisfies(trip -> {
			assertThat(trip.getDestination()).isEqualTo("Италия, Рим");
			assertThat(trip.getOrigin()).isEqualTo("Москва");
			assertThat(trip.getDateFrom()).isEqualTo(LocalDate.of(2027, 5, 12));
			assertThat(trip.getDateTo()).isEqualTo(LocalDate.of(2027, 5, 18));
			assertThat(trip.getAdults()).isEqualTo(2);
			assertThat(trip.getChildren()).isEqualTo(2);
			assertThat(trip.getChildAges()).containsExactly(5, 8);
			assertThat(trip.getPets()).isEqualTo(Pets.DOG);
			assertThat(trip.getBudget()).isEqualTo(Budget.UP_TO_6000);
		});
		assertThat(dialog.isActive(user)).isFalse();
	}

	@Test
	void withoutChildrenAgeQuestionIsSkipped() {
		answer("Казань", "Москва", "10.06-12.06", "1");

		assertThat(dialog.handle(user, "Нет").text()).contains("питомца");
	}

	@Test
	void invalidAnswerRepeatsQuestionWithHint() {
		answer("Сочи", "Москва");

		BotReply reply = dialog.handle(user, "в мае");

		assertThat(reply.text()).startsWith("⚠️").contains("Не получилось понять даты").contains("Когда");
		assertThat(conversations.findById(user)).get().extracting(Conversation::getStep).isEqualTo(DialogStep.DATES);
	}

	@Test
	void wrongNumberOfChildAgesIsRejected() {
		answer("Сочи", "Москва", "10.06-12.06", "2", "2");

		assertThat(dialog.handle(user, "5").text()).contains("каждого ребёнка");
	}

	@Test
	void questionnaireSurvivesBetweenCalls() {
		answer("Сочи");

		// как будто бот перезапустился: состояние читается из базы
		assertThat(dialog.isActive(user)).isTrue();
		assertThat(conversations.findById(user)).get().extracting(Conversation::getStep).isEqualTo(DialogStep.ORIGIN);
	}

	@Test
	void cancelRemovesDraft() {
		answer("Сочи", "Москва");

		BotReply reply = dialog.handle(user, NewTripDialog.CANCEL);

		assertThat(reply.text()).contains("отменено");
		assertThat(dialog.isActive(user)).isFalse();
		assertThat(trips.findAll()).noneMatch(trip -> trip.getUserId() == user);
	}

	@Test
	void restartOnConfirmStepBeginsAgain() {
		answer("Сочи", "Москва", "10.06-12.06", "1", "Нет", Pets.NONE.label(), Budget.ANY.label());

		assertThat(dialog.handle(user, NewTripDialog.RESTART).text()).contains("Куда едем");
		assertThat(trips.findAll().stream().filter(trip -> trip.getUserId() == user)).hasSize(1);
	}

	@Test
	void freeInputQuestionsKeepCancelButton() {
		BotReply question = dialog.start(user);

		assertThat(question.keyboard()).isEqualTo(Keyboard.options(List.of(List.of(NewTripDialog.CANCEL))));
	}

	private void answer(String... answers) {
		dialog.start(user);
		for (String text : answers) {
			dialog.handle(user, text);
		}
	}
}
