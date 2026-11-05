package com.eventhub.service;

import com.eventhub.dto.ApiDtos.BookingRequest;
import com.eventhub.model.Event;
import com.eventhub.model.Role;
import com.eventhub.model.User;
import com.eventhub.repository.EventRepository;
import com.eventhub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:booking-persistence;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.jwt.secret=integration-test-secret-that-is-at-least-32-bytes",
        "app.cors.allowed-origin=http://localhost:5500"
})
class BookingPersistenceIntegrationTest {
    @Autowired BookingService bookingService;
    @Autowired EventRepository events;
    @Autowired UserRepository users;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void confirmsMultiplePaidBookingsAndReservesInventory() {
        User organizer = users.save(new User("Organizer", "host-" + UUID.randomUUID() + "@example.test",
                "hash", Role.ORGANIZER));
        Event event = events.save(new Event("Test event", "Booking test", "Music",
                LocalDate.now().plusDays(30), LocalTime.NOON, "Test venue", null,
                new BigDecimal("499.00"), 10, organizer));

        User first = users.save(new User("First guest", "first-" + UUID.randomUUID() + "@example.test",
                "hash", Role.USER));
        User second = users.save(new User("Second guest", "second-" + UUID.randomUUID() + "@example.test",
                "hash", Role.USER));

        var firstBooking = bookingService.createBooking(first.getEmail(), new BookingRequest(event.getId(), 1));
        var secondBooking = bookingService.createBooking(second.getEmail(), new BookingRequest(event.getId(), 1));

        assertEquals("CONFIRMED", firstBooking.bookingStatus().name());
        assertEquals("CONFIRMED", secondBooking.bookingStatus().name());
        assertEquals(8, events.findById(event.getId()).orElseThrow().getAvailableTickets());
    }

    @Test
    void authenticatedBookingIsSavedAndVisibleOnlyToItsOwner() throws Exception {
        User organizer = users.save(new User("Organizer", "host-" + UUID.randomUUID() + "@example.test",
                "hash", Role.ORGANIZER));
        Event event = events.save(new Event("Persistent event", "Booking test", "Music",
                LocalDate.now().plusDays(30), LocalTime.NOON, "Test venue", "https://example.test/event.jpg",
                new BigDecimal("499.00"), 10, organizer));
        String firstEmail = "first-" + UUID.randomUUID() + "@example.test";
        String firstToken = registerUserAndGetToken(firstEmail);
        String secondToken = registerUserAndGetToken("second-" + UUID.randomUUID() + "@example.test");

        String bookingResponse = mvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":" + event.getId() + ",\"ticketQuantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventName").value("Persistent event"))
                .andExpect(jsonPath("$.imageUrl").value("https://example.test/event.jpg"))
                .andExpect(jsonPath("$.ticketPrice").value(499.0))
                .andExpect(jsonPath("$.totalPrice").value(998.0))
                .andExpect(jsonPath("$.ticketQuantity").value(2))
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"))
                .andReturn().getResponse().getContentAsString();
        JsonNode booking = mapper.readTree(bookingResponse);

        mvc.perform(get("/api/bookings/my").header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(booking.get("id").asLong()));
        mvc.perform(get("/api/bookings/my").header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    private String registerUserAndGetToken(String email) throws Exception {
        int phoneSuffix = Character.digit(UUID.randomUUID().toString().charAt(0), 16) % 10;
        String phoneNumber = "+91987654321" + phoneSuffix;
        var result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Booking Guest\",\"email\":\"" + email
                                + "\",\"phoneNumber\":\"" + phoneNumber
                                + "\",\"password\":\"SecurePass123\"}"))
                .andReturn();
        assertEquals(201, result.getResponse().getStatus(), result.getResponse().getContentAsString());
        String response = result.getResponse().getContentAsString();
        return mapper.readTree(response).get("token").asText();
    }
}
