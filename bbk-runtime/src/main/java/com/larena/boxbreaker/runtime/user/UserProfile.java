package com.larena.boxbreaker.runtime.user;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.EnumSet;
import java.util.Set;

/**
 * A user profile — the IBM&nbsp;i security object you sign on with. Holds the
 * (hashed) password, the enabled/disabled status and the set of special
 * authorities. The authenticated profile becomes the {@code user_profile} that
 * the jobs you start run under.
 */
@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String name;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserProfileStatus status = UserProfileStatus.ENABLED;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_profile_authority", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "authority", length = 16)
    private Set<SpecialAuthority> specialAuthorities = EnumSet.noneOf(SpecialAuthority.class);

    protected UserProfile() {}   // for JPA

    public UserProfile(String name, String passwordHash, Set<SpecialAuthority> specialAuthorities) {
        this.name = name;
        this.passwordHash = passwordHash;
        this.specialAuthorities = EnumSet.copyOf(specialAuthorities.isEmpty()
            ? EnumSet.noneOf(SpecialAuthority.class) : specialAuthorities);
    }

    public boolean isEnabled() { return status == UserProfileStatus.ENABLED; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPasswordHash() { return passwordHash; }
    public UserProfileStatus getStatus() { return status; }
    public Set<SpecialAuthority> getSpecialAuthorities() { return specialAuthorities; }

    public void setStatus(UserProfileStatus status) { this.status = status; }
}
