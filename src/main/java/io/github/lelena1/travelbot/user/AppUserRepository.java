package io.github.lelena1.travelbot.user;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	List<AppUser> findAllByOrderByCreatedAtAsc();
}
