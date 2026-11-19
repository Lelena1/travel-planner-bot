package io.github.lelena1.travelbot.user;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lelena1.travelbot.config.AccessProperties;

/**
 * Белый список: решает, кому можно пользоваться ботом.
 * Бот личный, поэтому все незнакомые пользователи получают отказ.
 */
@Service
public class AccessService {

	private static final Logger log = LoggerFactory.getLogger(AccessService.class);

	private final AppUserRepository repository;
	private final AccessProperties properties;

	public AccessService(AppUserRepository repository, AccessProperties properties) {
		this.repository = repository;
		this.properties = properties;
	}

	/** Переносит ID из настроек (переменные окружения) в базу при старте. */
	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void bootstrapFromConfig() {
		for (Long id : properties.admins()) {
			AppUser user = repository.findById(id).orElseGet(() -> new AppUser(id, Role.ADMIN));
			user.setRole(Role.ADMIN);
			repository.save(user);
		}
		for (Long id : properties.users()) {
			if (!repository.existsById(id)) {
				repository.save(new AppUser(id, Role.USER));
			}
		}
		log.info("Белый список загружен: {} пользователей", repository.count());
	}

	@Transactional(readOnly = true)
	public boolean isAllowed(long telegramId) {
		return repository.existsById(telegramId);
	}

	@Transactional(readOnly = true)
	public boolean isAdmin(long telegramId) {
		return repository.findById(telegramId).map(AppUser::isAdmin).orElse(false);
	}

	/** @return true, если пользователь добавлен; false, если он уже был в списке */
	@Transactional
	public boolean allow(long telegramId) {
		if (repository.existsById(telegramId)) {
			return false;
		}
		repository.save(new AppUser(telegramId, Role.USER));
		return true;
	}

	@Transactional
	public DenyResult deny(long telegramId) {
		return repository.findById(telegramId).map(user -> {
			if (user.isAdmin()) {
				return DenyResult.IS_ADMIN;
			}
			repository.delete(user);
			return DenyResult.REMOVED;
		}).orElse(DenyResult.NOT_FOUND);
	}

	@Transactional(readOnly = true)
	public List<AppUser> listUsers() {
		return repository.findAllByOrderByCreatedAtAsc();
	}

	public enum DenyResult {
		REMOVED, NOT_FOUND, IS_ADMIN
	}
}
