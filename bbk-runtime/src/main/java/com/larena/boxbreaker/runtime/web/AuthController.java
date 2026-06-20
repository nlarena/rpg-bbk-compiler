package com.larena.boxbreaker.runtime.web;

import com.larena.boxbreaker.runtime.security.JwtService;
import com.larena.boxbreaker.runtime.user.UserProfile;
import com.larena.boxbreaker.runtime.user.UserProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Sign-on: {@code POST /api/auth/login} issues a JWT; {@code GET /api/auth/me} echoes the caller. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserProfileService users;
    private final JwtService jwt;

    public AuthController(UserProfileService users, JwtService jwt) {
        this.users = users;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        UserProfile profile = users.authenticate(request.user(), request.password());
        List<String> authorities = profile.getSpecialAuthorities().stream().map(Enum::name).sorted().toList();
        String token = jwt.issue(profile.getName(), authorities);
        return new LoginResponse(token, profile.getName(), authorities);
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        List<String> authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority).sorted().toList();
        return new MeResponse(authentication.getName(), authorities);
    }

    public record LoginRequest(String user, String password) {}
    public record LoginResponse(String token, String user, List<String> authorities) {}
    public record MeResponse(String user, List<String> authorities) {}
}
