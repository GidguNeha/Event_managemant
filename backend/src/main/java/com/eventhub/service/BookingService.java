package com.eventhub.service;

import com.eventhub.dto.ApiDtos.*;
import com.eventhub.model.*;
import com.eventhub.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {
    private final BookingRepository bookings;
    private final EventRepository events;
    private final UserRepository users;

    public BookingService(BookingRepository bookings, EventRepository events, UserRepository users) {
        this.bookings = bookings;
        this.events = events;
        this.users = users;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingView createBooking(String email, BookingRequest request) {
        User user = user(email);
        Event event = events.findByIdForUpdate(request.eventId())
                .orElseThrow(() -> DomainException.notFound("Event not found"));
        validateBookableEvent(event);
        long existingTickets = bookings.sumTicketQuantityForUserAndEventInStatuses(
                event.getId(), user.getId(), List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
        if (existingTickets + request.ticketQuantity() > 20) {
            throw DomainException.conflict("You can book at most 20 tickets for an event");
        }
        if (event.getAvailableTickets() < request.ticketQuantity()) {
            throw DomainException.conflict("Not enough tickets are available");
        }
        event.setAvailableTickets(event.getAvailableTickets() - request.ticketQuantity());
        BigDecimal totalPrice = event.getTicketPrice().multiply(BigDecimal.valueOf(request.ticketQuantity()));
        Booking booking = new Booking(user, event, request.ticketQuantity(), totalPrice,
                UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking = bookings.saveAndFlush(booking);
        return BookingView.from(booking);
    }

    public List<BookingView> mine(String email) {
        User user = user(email);
        return bookings.findByUserIdOrderByBookedAtDesc(user.getId()).stream().map(BookingView::from).toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingView cancel(String email, Long bookingId) {
        User actor = user(email);
        Booking snapshot = bookings.findById(bookingId)
                .orElseThrow(() -> DomainException.notFound("Booking not found"));
        authorizeBooking(actor, snapshot);
        Event event = events.findByIdForUpdate(snapshot.getEvent().getId())
                .orElseThrow(() -> DomainException.notFound("Event not found"));
        Booking booking = bookings.findByIdForUpdate(bookingId)
                .orElseThrow(() -> DomainException.notFound("Booking not found"));
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw DomainException.conflict("Booking is already cancelled");
        }
        if (event.getEventDate().atTime(event.getStartTime())
                .isBefore(LocalDateTime.now(ZoneId.systemDefault()))) {
            throw DomainException.conflict("Past event bookings cannot be cancelled");
        }
        event.setAvailableTickets(event.getAvailableTickets() + booking.getTicketQuantity());
        booking.setBookingStatus(BookingStatus.CANCELLED);
        return BookingView.from(booking);
    }

    public BookingView get(String email, Long id) {
        User actor = user(email);
        Booking booking = bookings.findById(id).orElseThrow(() -> DomainException.notFound("Booking not found"));
        authorizeBooking(actor, booking);
        return BookingView.from(booking);
    }

    public List<BookingView> all() {
        return bookings.findAll().stream().map(BookingView::from).toList();
    }

    public List<BookingView> organizerAttendees(String email, Long eventId) {
        User actor = user(email);
        Event event = events.findById(eventId).orElseThrow(() -> DomainException.notFound("Event not found"));
        if (actor.getRole() != Role.ADMIN && !event.getOrganizer().getId().equals(actor.getId())) {
            throw DomainException.forbidden("You can view attendees only for your own events");
        }
        return bookings.findByEventIdOrderByBookedAtDesc(eventId).stream().map(BookingView::from).toList();
    }

    private void validateBookableEvent(Event event) {
        if (event.isCancelled()) throw DomainException.conflict("This event has been cancelled");
        if (event.getEventDate().atTime(event.getStartTime())
                .isBefore(LocalDateTime.now(ZoneId.systemDefault()))) {
            throw DomainException.conflict("This event has already taken place");
        }
    }

    private void authorizeBooking(User actor, Booking booking) {
        if (actor.getRole() != Role.ADMIN && !booking.getUser().getId().equals(actor.getId())) {
            throw DomainException.forbidden("You can access only your own booking");
        }
    }

    private User user(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> DomainException.notFound("User not found"));
    }
}
