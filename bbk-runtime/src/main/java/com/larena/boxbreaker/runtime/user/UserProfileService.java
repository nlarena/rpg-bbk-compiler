package com.larena.boxbreaker.runtime.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/** Create user profiles (hashing the password) and authenticate sign-on. */
@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    private final UserProfileRepository repo;
    private final PasswordEncoder passwordEncoder;

    public UserProfileService(UserProfileRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserProfile create(String name, String rawPassword, Set<SpecialAuthority> authorities) {
        String profile = normalize(name);
        if (repo.existsByName(profile)) {
            throw new IllegalArgumentException("user profile '" + profile + "' already exists");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("password must not be blank");
        }
        return repo.save(new UserProfile(profile, passwordEncoder.encode(rawPassword), authorities));
    }

    /** Validate sign-on; throws {@link AuthenticationFailedException} on any failure. */
    @Transactional(readOnly = true)
    public UserProfile authenticate(String name, String rawPassword) {
        String profileName = normalize(name);
        UserProfile profile = repo.findByName(profileName).orElse(null);
        if (profile == null) {
            log.warn("Sign-on rejected: unknown user '{}'", profileName);
            throw new AuthenticationFailedException("invalid user or password");
        }
        if (!profile.isEnabled()) {
            log.warn("Sign-on rejected: user '{}' is disabled", profileName);
            throw new AuthenticationFailedException("user profile is disabled");
        }
        if (rawPassword == null || !passwordEncoder.matches(rawPassword, profile.getPasswordHash())) {
            log.warn("Sign-on rejected: bad password for user '{}'", profileName);
            throw new AuthenticationFailedException("invalid user or password");
        }
        log.info("Sign-on OK: user '{}' authorities={}", profileName, profile.getSpecialAuthorities());
        return profile;
    }

    @Transactional(readOnly = true)
    public List<UserProfile> list() {
        return repo.findAll();
    }

    private static String normalize(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("user name must not be blank");
        return name.trim().toUpperCase();
    }
}
