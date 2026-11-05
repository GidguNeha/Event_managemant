package com.eventhub.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:eventhub;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.jwt.secret=integration-test-secret-that-is-at-least-32-bytes",
        "app.jwt.expiration-ms=60000",
        "app.cors.allowed-origin=http://localhost:5500"
})
@AutoConfigureMockMvc
class AuthApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void registrationReturnsJwtThatAuthenticatesCurrentUser() throws Exception {
        String response = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Integration Guest","email":"integration@example.test","phoneNumber":"+919876543210","password":"SecurePass123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andReturn().getResponse().getContentAsString();
        JsonNode body = mapper.readTree(response);

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + body.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("integration@example.test"));
        mvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + body.get("token").asText()))
                .andExpect(status().isForbidden());
        String loginResponse = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"integration@example.test","password":"SecurePass123"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode login = mapper.readTree(loginResponse);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + login.get("token").asText()))
                .andExpect(status().isOk());
    }

    @Test
    void eventBrowsingIsPublic() throws Exception {
        mvc.perform(get("/api/events")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void privateBookingEndpointRejectsAnonymousRequests() throws Exception {
        mvc.perform(get("/api/bookings/my")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":1,\"ticketQuantity\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidCredentialsAreRejected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"missing@example.test","password":"WrongPassword123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateRegistrationReturnsConflict() throws Exception {
        String body = """
                {"fullName":"Duplicate Guest","email":"duplicate@example.test","phoneNumber":"+919876543210","password":"SecurePass123"}
                """;
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }
}
