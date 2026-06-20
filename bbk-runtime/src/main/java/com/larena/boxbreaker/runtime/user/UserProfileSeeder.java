package com.larena.boxbreaker.runtime.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

/**
 * Seeds the security-officer profile on startup if it is missing, so the system
 * has someone to sign on as (mirrors IBM&nbsp;i's {@code QSECOFR}). The seeded
 * profile gets {@code *ALLOBJ} + {@code *SECADM}.
 */
@Component
public class UserProfileSeeder implements CommandLineRunner {

    private final UserProfileService users;
    private final UserProfileRepository repo;
    private final String seedUser;
    private final String seedPassword;

    public UserProfileSeeder(UserProfileService users, UserProfileRepository repo,
                             @Value("${bbk.security.seed.user}") String seedUser,
                             @Value("${bbk.security.seed.password}") String seedPassword) {
        this.users = users;
        this.repo = repo;
        this.seedUser = seedUser;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(String... args) {
        String name = seedUser.trim().toUpperCase();
        if (!repo.existsByName(name)) {
            users.create(name, seedPassword, EnumSet.of(SpecialAuthority.ALLOBJ, SpecialAuthority.SECADM));
        }
    }
}
