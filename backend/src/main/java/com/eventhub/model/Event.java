package com.eventhub.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "events")
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 160)
    private String name;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false, length = 80)
    private String category;
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    @Column(nullable = false, length = 200)
    private String venue;
    @Column(name = "image_url", length = 1000)
    private String imageUrl;
    @Column(name = "ticket_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal ticketPrice;
    @Column(name = "total_tickets", nullable = false)
    private int totalTickets;
    @Column(name = "available_tickets", nullable = false)
    private int availableTickets;
    @Column(nullable = false)
    private boolean cancelled = false;
    @ManyToOne(optional = false)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Event() {}
    public Event(String name, String description, String category, LocalDate eventDate, LocalTime startTime,
                 String venue, String imageUrl, BigDecimal ticketPrice, int totalTickets, User organizer) {
        this.name = name; this.description = description; this.category = category; this.eventDate = eventDate;
        this.startTime = startTime; this.venue = venue; this.imageUrl = imageUrl; this.ticketPrice = ticketPrice;
        this.totalTickets = totalTickets; this.availableTickets = totalTickets; this.organizer = organizer;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public BigDecimal getTicketPrice() { return ticketPrice; }
    public void setTicketPrice(BigDecimal ticketPrice) { this.ticketPrice = ticketPrice; }
    public int getTotalTickets() { return totalTickets; }
    public int getAvailableTickets() { return availableTickets; }
    public void setAvailableTickets(int availableTickets) { this.availableTickets = availableTickets; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    public void setTotalTickets(int totalTickets) { this.totalTickets = totalTickets; }
    public User getOrganizer() { return organizer; }
    public Instant getCreatedAt() { return createdAt; }
}
