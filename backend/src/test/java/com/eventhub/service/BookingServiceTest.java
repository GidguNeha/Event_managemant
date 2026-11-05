package com.eventhub.service;

import com.eventhub.dto.ApiDtos.BookingRequest;
import com.eventhub.model.*;
import com.eventhub.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock BookingRepository bookings;
    @Mock EventRepository events;
    @Mock UserRepository users;
    private BookingService service;
    private User attendee;
    private Event event;

    @BeforeEach
    void setUp() {
        service = new BookingService(bookings, events, users);
        attendee = new User("Taylor Guest", "taylor@example.test", "hash", Role.USER);
        event = new Event("Spring Sounds", "Live music", "Music", LocalDate.now().plusDays(14),
                LocalTime.of(19, 0), "Main Hall", null, new BigDecimal("12.50"), 5,
                new User("Organizer", "host@example.test", "hash", Role.ORGANIZER));
        when(users.findByEmailIgnoreCase("taylor@example.test")).thenReturn(Optional.of(attendee));
    }

    @Test
    void bookingConfirmsPaidEventTicketsWithoutPayment() {
        when(events.findByIdForUpdate(7L)).thenReturn(Optional.of(event));
        when(bookings.sumTicketQuantityForUserAndEventInStatuses(isNull(), isNull(),
                eq(java.util.List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)))).thenReturn(0L);
        when(bookings.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createBooking("taylor@example.test", new BookingRequest(7L, 2));

        assertEquals(new BigDecimal("25.00"), event.getTicketPrice().multiply(BigDecimal.valueOf(2)));
        assertEquals(BookingStatus.CONFIRMED, result.bookingStatus());
        assertEquals(new BigDecimal("25.00"), result.totalPrice());
        assertEquals(3, event.getAvailableTickets());
        verify(bookings, never()).save(any(Booking.class));
    }

    @Test
    void rejectsBookingWhenCapacityIsInsufficient() {
        when(events.findByIdForUpdate(7L)).thenReturn(Optional.of(event));
        var error = assertThrows(DomainException.class,
                () -> service.createBooking("taylor@example.test", new BookingRequest(7L, 6)));

        assertEquals(409, error.getStatus().value());
        assertEquals(5, event.getAvailableTickets());
        verify(bookings, never()).saveAndFlush(any(Booking.class));
    }

    @Test
    void rejectsBookingForPastEvents() {
        Event past = new Event("Past show", "Already happened", "Music", LocalDate.now().minusDays(1),
                LocalTime.NOON, "Main Hall", null, BigDecimal.TEN, 5, event.getOrganizer());
        when(events.findByIdForUpdate(7L)).thenReturn(Optional.of(past));

        assertThrows(DomainException.class,
                () -> service.createBooking("taylor@example.test", new BookingRequest(7L, 1)));
        verify(bookings, never()).saveAndFlush(any(Booking.class));
    }

    @Test
    void capsConfirmedTicketsPerAttendeeAndEvent() {
        when(events.findByIdForUpdate(7L)).thenReturn(Optional.of(event));
        when(bookings.sumTicketQuantityForUserAndEventInStatuses(isNull(), isNull(),
                eq(java.util.List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)))).thenReturn(19L);

        assertThrows(DomainException.class,
                () -> service.createBooking("taylor@example.test", new BookingRequest(7L, 2)));
        assertEquals(5, event.getAvailableTickets());
        verify(bookings, never()).saveAndFlush(any(Booking.class));
    }

    @Test
    void freeBookingIsConfirmed() {
        Event freeEvent = new Event("Free show", "Community event", "Community", LocalDate.now().plusDays(2),
                LocalTime.NOON, "Main Hall", null, BigDecimal.ZERO, 5, event.getOrganizer());
        when(events.findByIdForUpdate(7L)).thenReturn(Optional.of(freeEvent));
        when(bookings.sumTicketQuantityForUserAndEventInStatuses(isNull(), isNull(),
                eq(java.util.List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)))).thenReturn(0L);
        when(bookings.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.createBooking("taylor@example.test", new BookingRequest(7L, 1));

        assertEquals(BookingStatus.CONFIRMED, result.bookingStatus());
    }
}
