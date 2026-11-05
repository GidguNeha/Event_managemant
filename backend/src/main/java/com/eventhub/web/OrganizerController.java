package com.eventhub.web;

import com.eventhub.dto.ApiDtos.*;
import com.eventhub.repository.BookingRepository;
import com.eventhub.repository.EventRepository;
import com.eventhub.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/organizer")
public class OrganizerController {
    private final EventService events;
    private final BookingService bookings;
    private final AuthService auth;
    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    public OrganizerController(EventService events, BookingService bookings, AuthService auth,
                               EventRepository eventRepository, BookingRepository bookingRepository) {
        this.events = events; this.bookings = bookings; this.auth = auth;
        this.eventRepository = eventRepository; this.bookingRepository = bookingRepository;
    }
    @GetMapping("/events") public List<EventView> events(@AuthenticationPrincipal String email) { return events.organizerEvents(email); }
    @GetMapping("/events/{id}/attendees") public List<BookingView> attendees(@AuthenticationPrincipal String email, @PathVariable Long id) {
        return bookings.organizerAttendees(email, id);
    }
    @GetMapping("/dashboard") public Map<String, Long> dashboard(@AuthenticationPrincipal String email) {
        Long organizerId = auth.requireUser(email).getId();
        return Map.of("events", (long) eventRepository.findByOrganizerIdOrderByEventDateAsc(organizerId).size(),
                "bookings", bookingRepository.countByEventOrganizerId(organizerId));
    }
}
