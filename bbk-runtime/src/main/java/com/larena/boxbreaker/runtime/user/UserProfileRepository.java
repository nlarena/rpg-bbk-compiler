package com.larena.boxbreaker.runtime.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Persistence for {@link UserProfile}s. */
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByName(String name);

    boolean existsByName(String name);
}
