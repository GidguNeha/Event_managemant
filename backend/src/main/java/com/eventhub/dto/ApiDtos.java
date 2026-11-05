package com.eventhub.dto;

import com.eventhub.model.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public final class ApiDtos {
    private ApiDtos() {}

    public record RegisterRequest(@NotBlank @Size(max = 120) String fullName,
                                  @NotBlank @Email @Size(max = 190) String email,
                                  @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$",
                                          message = "Enter a phone number in international format, such as +919876543210")
                                  String phoneNumber,
                                  @NotBlank @Size(min = 8, max = 72) String password) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record AuthResponse(String token, UserView user) {}
    public record UserView(Long id, String fullName, String email, String phoneNumber, Role role, Instant createdAt) {
        public static UserView from(User user) {
            return new UserView(user.getId(), user.getFullName(), user.getEmail(), user.getPhoneNumber(),
                    user.getRole(), user.getCreatedAt());
        }
    }
    public record EventRequest(@NotBlank @Size(max = 160) String name,
                               @NotBlank @Size(max = 10000) String description,
                               @NotBlank @Size(max = 80) String category,
                               @NotNull @FutureOrPresent LocalDate eventDate,
                               @NotNull LocalTime startTime,
                               @NotBlank @Size(max = 200) String venue,
                               @Size(max = 1000) String imageUrl,
                               @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal ticketPrice,
                               @NotNull @Min(1) @Max(1000000) Integer totalTickets) {}
    public record EventView(Long id, String name, String description, String category, LocalDate eventDate,
                            LocalTime startTime, String venue, String imageUrl, BigDecimal ticketPrice,
                            int totalTickets, int availableTickets, Long organizerId, String organizerName, boolean cancelled) {
        public static EventView from(Event event) {
            return new EventView(event.getId(), event.getName(), event.getDescription(), event.getCategory(),
                    event.getEventDate(), event.getStartTime(), event.getVenue(), event.getImageUrl(),
                    event.getTicketPrice(), event.getTotalTickets(), event.getAvailableTickets(),
                    event.getOrganizer().getId(), event.getOrganizer().getFullName(), event.isCancelled());
        }
    }
    public record BookingRequest(@NotNull @Positive Long eventId, @NotNull @Positive @Max(20) Integer ticketQuantity) {}
    public record BookingView(Long id, String bookingReference, Long eventId, String eventName, String category,
                              String imageUrl, LocalDate eventDate, LocalTime startTime, String venue,
                              int ticketQuantity, BigDecimal ticketPrice, BigDecimal totalPrice,
                              BookingStatus bookingStatus, Instant bookedAt,
                              Long userId, String userName, String userEmail) {
        public static BookingView from(Booking booking) {
            return new BookingView(booking.getId(), booking.getBookingReference(), booking.getEvent().getId(),
                    booking.getEvent().getName(), booking.getEvent().getCategory(), booking.getEvent().getImageUrl(),
                    booking.getEvent().getEventDate(), booking.getEvent().getStartTime(), booking.getEvent().getVenue(),
                    booking.getTicketQuantity(), booking.getEvent().getTicketPrice(), booking.getTotalPrice(),
                    booking.getBookingStatus(),
                    booking.getBookedAt(), booking.getUser().getId(), booking.getUser().getFullName(),
                    booking.getUser().getEmail());
        }
    }
    public record ProfileRequest(@NotBlank @Size(max = 120) String fullName,
                                @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{7,14}$",
                                        message = "Enter a phone number in international format, such as +919876543210")
                                String phoneNumber) {}
    public record RoleRequest(@NotNull Role role) {}
    public record PasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 72) String newPassword) {}
    public record Message(String message) {}
}
