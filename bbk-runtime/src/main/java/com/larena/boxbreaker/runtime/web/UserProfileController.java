package com.larena.boxbreaker.runtime.web;

import com.larena.boxbreaker.runtime.user.SpecialAuthority;
import com.larena.boxbreaker.runtime.user.UserProfile;
import com.larena.boxbreaker.runtime.user.UserProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * User-profile administration — requires the {@code *SECADM} special authority
 * (the IBM&nbsp;i security officer surface). Demonstrates per-endpoint
 * authorization on top of JWT authentication.
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAuthority('SECADM') or hasAuthority('ALLOBJ')")
public class UserProfileController {

    private final UserProfileService users;

    public UserProfileController(UserProfileService users) {
        this.users = users;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@RequestBody CreateUserRequest request) {
        UserProfile profile = users.create(request.name(), request.password(), parse(request.authorities()));
        return UserResponse.of(profile);
    }

    @GetMapping
    public List<UserResponse> list() {
        return users.list().stream().map(UserResponse::of).toList();
    }

    private static Set<SpecialAuthority> parse(List<String> names) {
        Set<SpecialAuthority> result = new LinkedHashSet<>();
        if (names != null) {
            for (String n : names) result.add(SpecialAuthority.valueOf(n.trim().toUpperCase()));
        }
        return result;
    }

    public record CreateUserRequest(String name, String password, List<String> authorities) {}

    public record UserResponse(String name, String status, List<String> authorities) {
        static UserResponse of(UserProfile p) {
            return new UserResponse(p.getName(), p.getStatus().name(),
                p.getSpecialAuthorities().stream().map(Enum::name).sorted().toList());
        }
    }
}
