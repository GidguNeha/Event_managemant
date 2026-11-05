package com.eventhub.web;

import com.eventhub.dto.ApiDtos.*;
import com.eventhub.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService service;
    public BookingController(BookingService service) { this.service = service; }
    @PostMapping
    public BookingView createBooking(@AuthenticationPrincipal String email,
                                     @Valid @RequestBody BookingRequest request) {
        return service.createBooking(email, request);
    }
    @GetMapping("/my") public List<BookingView> mine(@AuthenticationPrincipal String email) { return service.mine(email); }
    @GetMapping("/{id}") public BookingView get(@AuthenticationPrincipal String email, @PathVariable Long id) {
        return service.get(email, id);
    }
    @PatchMapping("/{id}/cancel") public BookingView cancel(@AuthenticationPrincipal String email, @PathVariable Long id) {
        return service.cancel(email, id);
    }
}
