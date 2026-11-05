package com.eventhub.service;

import com.eventhub.dto.ApiDtos.*;
import com.eventhub.model.*;
import com.eventhub.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.util.List;

@Service
public class EventService {
    private final EventRepository events;
    private final UserRepository users;
    private final BookingRepository bookings;
    public EventService(EventRepository events, UserRepository users, BookingRepository bookings) {
        this.events = events; this.users = users; this.bookings = bookings;
    }
    public List<EventView> list(String search, String category) {
        return events.findAll().stream()
                .filter(e -> !e.isCancelled())
                .filter(e -> !e.getEventDate().isBefore(java.time.LocalDate.now()))
                .filter(e -> search == null || e.getName().toLowerCase().contains(search.toLowerCase()))
                .filter(e -> category == null || category.isBlank() || e.getCategory().equalsIgnoreCase(category))
                .sorted(java.util.Comparator.comparing(Event::getEventDate).thenComparing(Event::getStartTime))
                .map(EventView::from).toList();
    }
    public EventView get(Long id) {
        Event event = requireEvent(id);
        if (event.isCancelled()) throw DomainException.conflict("This event has been cancelled");
        return EventView.from(event);
    }
    @Transactional
    public EventView create(String email, EventRequest request) {
        User organizer = user(email);
        if (organizer.getRole() != Role.ORGANIZER && organizer.getRole() != Role.ADMIN)
            throw DomainException.forbidden("Organizer role required");
        Event event = new Event(request.name().trim(), request.description().trim(), request.category().trim(),
                request.eventDate(), request.startTime(), request.venue().trim(), request.imageUrl(),
                request.ticketPrice(), request.totalTickets(), organizer);
        return EventView.from(events.save(event));
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public EventView update(String email, Long id, EventRequest request) {
        User actor = user(email);
        Event event = events.findByIdForUpdate(id).orElseThrow(() -> DomainException.notFound("Event not found"));
        authorize(actor, event);
        if (event.isCancelled()) throw DomainException.conflict("Cancelled events cannot be edited");
        int sold = event.getTotalTickets() - event.getAvailableTickets();
        if (request.totalTickets() < sold) throw DomainException.conflict("Capacity cannot be lower than tickets already booked");
        event.setName(request.name().trim()); event.setDescription(request.description().trim());
        event.setCategory(request.category().trim()); event.setEventDate(request.eventDate());
        event.setStartTime(request.startTime()); event.setVenue(request.venue().trim());
        event.setImageUrl(request.imageUrl()); event.setTicketPrice(request.ticketPrice());
        event.setTotalTickets(request.totalTickets()); event.setAvailableTickets(request.totalTickets() - sold);
        return EventView.from(event);
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(String email, Long id) {
        User actor = user(email);
        Event event = events.findByIdForUpdate(id).orElseThrow(() -> DomainException.notFound("Event not found"));
        authorize(actor, event);
        if (bookings.existsByEventId(id)) throw DomainException.conflict("Events with booking history cannot be deleted");
        events.delete(event);
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public EventView cancel(Long id) {
        Event event = events.findByIdForUpdate(id).orElseThrow(() -> DomainException.notFound("Event not found"));
        if (event.isCancelled()) throw DomainException.conflict("Event is already cancelled");
        event.setCancelled(true);
        event.setAvailableTickets(event.getTotalTickets());
        bookings.findByEventIdOrderByBookedAtDesc(id).stream()
                .filter(booking -> booking.getBookingStatus() != BookingStatus.CANCELLED)
                .forEach(booking -> booking.setBookingStatus(BookingStatus.CANCELLED));
        return EventView.from(event);
    }
    public List<EventView> organizerEvents(String email) {
        User organizer = user(email);
        return events.findByOrganizerIdOrderByEventDateAsc(organizer.getId()).stream().map(EventView::from).toList();
    }
    public Event requireEvent(Long id) {
        return events.findById(id).orElseThrow(() -> DomainException.notFound("Event not found"));
    }
    private User user(String email) { return users.findByEmailIgnoreCase(email).orElseThrow(() -> DomainException.notFound("User not found")); }
    private void authorize(User actor, Event event) {
        if (actor.getRole() != Role.ADMIN && !event.getOrganizer().getId().equals(actor.getId()))
            throw DomainException.forbidden("You can manage only your own events");
    }
}
