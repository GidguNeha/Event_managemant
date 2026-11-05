package com.eventhub.service;

import com.eventhub.dto.ApiDtos.*;
import com.eventhub.model.*;
import com.eventhub.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private final UserRepository users;
    private final EventRepository events;
    private final BookingRepository bookings;
    public AdminService(UserRepository users, EventRepository events, BookingRepository bookings) {
        this.users = users; this.events = events; this.bookings = bookings;
    }
    public Map<String, Long> dashboard() {
        return Map.of("users", users.count(), "events", events.count(), "bookings", bookings.count(),
                "confirmedBookings", bookings.countByBookingStatus(BookingStatus.CONFIRMED));
    }
    public List<UserView> users() { return users.findAll().stream().map(UserView::from).toList(); }
    @Transactional
    public UserView changeRole(Long id, RoleRequest request) {
        User user = users.findById(id).orElseThrow(() -> DomainException.notFound("User not found"));
        user.setRole(request.role());
        return UserView.from(user);
    }
    public List<EventView> events() { return events.findAll().stream().map(EventView::from).toList(); }
}
