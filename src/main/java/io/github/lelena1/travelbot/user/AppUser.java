package io.github.lelena1.travelbot.user;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Пользователь, которому разрешён доступ к боту. */
@Entity
@Table(name = "app_user")
public class AppUser {

	@Id
	@Column(name = "telegram_id")
	private Long telegramId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private Role role;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected AppUser() {
		// для JPA
	}

	public AppUser(long telegramId, Role role) {
		this.telegramId = telegramId;
		this.role = role;
		this.createdAt = Instant.now();
	}

	public Long getTelegramId() {
		return telegramId;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public boolean isAdmin() {
		return role == Role.ADMIN;
	}
}
