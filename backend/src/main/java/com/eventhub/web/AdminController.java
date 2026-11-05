package com.eventhub.web;

import com.eventhub.dto.ApiDtos.*;
import com.eventhub.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService admin;
    private final BookingService bookings;
    private final EventService events;
    public AdminController(AdminService admin, BookingService bookings, EventService events) {
        this.admin = admin; this.bookings = bookings; this.events = events;
    }
    @GetMapping("/dashboard") public Map<String, Long> dashboard() { return admin.dashboard(); }
    @GetMapping("/users") public List<UserView> users() { return admin.users(); }
    @PatchMapping("/users/{id}/role") public UserView role(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return admin.changeRole(id, request);
    }
    @GetMapping("/events") public List<EventView> events() { return admin.events(); }
    @DeleteMapping("/events/{id}") public Message deleteEvent(@AuthenticationPrincipal String email, @PathVariable Long id) {
        events.delete(email, id); return new Message("Event deleted");
    }
    @PatchMapping("/events/{id}/cancel") public EventView cancelEvent(@PathVariable Long id) { return events.cancel(id); }
    @GetMapping("/bookings") public List<BookingView> bookings() { return bookings.all(); }
}
