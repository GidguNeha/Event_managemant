package com.eventhub.web;

import com.eventhub.dto.ApiDtos.*;
import com.eventhub.service.EventService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService service;
    public EventController(EventService service) { this.service = service; }
    @GetMapping public List<EventView> list(@RequestParam(required = false) String search,
                                            @RequestParam(required = false) String category) {
        return service.list(search, category);
    }
    @GetMapping("/{id}") public EventView get(@PathVariable Long id) { return service.get(id); }
    @PostMapping public EventView create(@AuthenticationPrincipal String email, @Valid @RequestBody EventRequest request) {
        return service.create(email, request);
    }
    @PutMapping("/{id}") public EventView update(@AuthenticationPrincipal String email, @PathVariable Long id,
                                                 @Valid @RequestBody EventRequest request) {
        return service.update(email, id, request);
    }
    @DeleteMapping("/{id}") public Message delete(@AuthenticationPrincipal String email, @PathVariable Long id) {
        service.delete(email, id); return new Message("Event deleted");
    }
}
