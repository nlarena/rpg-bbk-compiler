package com.larena.boxbreaker.runtime.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.larena.boxbreaker.runtime.user.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end sign-on + authorization against MySQL: JWT login, then per-endpoint
 * special-authority enforcement (user administration needs {@code *SECADM}).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserProfileRepository repo;

    private String login(String user, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("user", user, "password", password))))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    @Test
    void signOnAndPerEndpointAuthorization() throws Exception {
        // the seeded security officer can sign on
        String officer = login("QSECOFR", "qsecofr");

        // ... and create a profile (needs *SECADM)
        mvc.perform(post("/api/users").header("Authorization", "Bearer " + officer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                    "name", "authtmp", "password", "secret", "authorities", List.of("JOBCTL")))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("AUTHTMP"))
            .andExpect(jsonPath("$.authorities", hasItem("JOBCTL")));

        // the new user signs on but lacks *SECADM -> 403 on the admin endpoint
        String tmpUser = login("authtmp", "secret");
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + tmpUser))
            .andExpect(status().isForbidden());

        // the officer can list; an unauthenticated request is 401
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + officer)).andExpect(status().isOk());
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());

        // /me echoes the caller and its authorities
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + officer))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user").value("QSECOFR"))
            .andExpect(jsonPath("$.authorities", hasItem("SECADM")));

        // a bad password is rejected
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("user", "QSECOFR", "password", "wrong"))))
            .andExpect(status().isUnauthorized());

        // tidy up
        repo.findByName("AUTHTMP").ifPresent(p -> repo.deleteById(p.getId()));
    }
}
