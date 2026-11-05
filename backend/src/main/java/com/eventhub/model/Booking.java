package com.eventhub.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bookings", uniqueConstraints = @UniqueConstraint(name = "uk_bookings_reference", columnNames = "booking_reference"))
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
    @Column(name = "ticket_quantity", nullable = false)
    private int ticketQuantity;
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;
    @Enumerated(EnumType.STRING) @Column(name = "booking_status", nullable = false, length = 20,
            columnDefinition = "varchar(20)")
    private BookingStatus bookingStatus = BookingStatus.CONFIRMED;
    @Column(name = "booking_reference", nullable = false, length = 40)
    private String bookingReference;
    @Column(name = "booked_at", nullable = false, updatable = false)
    private Instant bookedAt = Instant.now();

    protected Booking() {}
    public Booking(User user, Event event, int ticketQuantity, BigDecimal totalPrice, String bookingReference) {
        this.user = user; this.event = event; this.ticketQuantity = ticketQuantity;
        this.totalPrice = totalPrice; this.bookingReference = bookingReference;
    }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public Event getEvent() { return event; }
    public int getTicketQuantity() { return ticketQuantity; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public BookingStatus getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(BookingStatus bookingStatus) { this.bookingStatus = bookingStatus; }
    public String getBookingReference() { return bookingReference; }
    public Instant getBookedAt() { return bookedAt; }
}
