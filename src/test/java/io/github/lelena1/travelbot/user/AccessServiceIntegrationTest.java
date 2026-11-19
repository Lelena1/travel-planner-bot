package io.github.lelena1.travelbot.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.lelena1.travelbot.TestcontainersConfiguration;
import io.github.lelena1.travelbot.user.AccessService.DenyResult;

/** Проверяет белый список на настоящей PostgreSQL (в Docker). Без Docker тест пропускается. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = { "access.admins=100", "access.users=200,201" })
@Import(TestcontainersConfiguration.class)
class AccessServiceIntegrationTest {

	@Autowired
	private AccessService access;

	@Test
	void whitelistFromConfigIsLoadedIntoDatabase() {
		assertThat(access.isAdmin(100)).isTrue();
		assertThat(access.isAllowed(200)).isTrue();
		assertThat(access.isAdmin(200)).isFalse();
		assertThat(access.isAllowed(999)).isFalse();
	}

	@Test
	void allowAndDenyUser() {
		assertThat(access.allow(300)).isTrue();
		assertThat(access.allow(300)).isFalse();
		assertThat(access.isAllowed(300)).isTrue();

		assertThat(access.deny(300)).isEqualTo(DenyResult.REMOVED);
		assertThat(access.isAllowed(300)).isFalse();
	}

	@Test
	void adminCannotBeDenied() {
		assertThat(access.deny(100)).isEqualTo(DenyResult.IS_ADMIN);
		assertThat(access.isAllowed(100)).isTrue();
	}
}
