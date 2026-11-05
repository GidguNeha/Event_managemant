package com.eventhub.config;

import com.eventhub.model.Event;
import com.eventhub.model.Role;
import com.eventhub.model.User;
import com.eventhub.repository.EventRepository;
import com.eventhub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@Configuration
@Profile("demo")
public class DemoDataConfiguration {
    @Bean
    ApplicationRunner seedDemoData(UserRepository users, EventRepository events, PasswordEncoder encoder,
                                   @Value("${app.demo.organizer-password}") String organizerPassword,
                                   @Value("${app.demo.admin-password}") String adminPassword) {
        return args -> {
            User organizer = users.findByEmailIgnoreCase("organizer@eventhub.local").orElseGet(() ->
                    users.save(new User("EventHub Organizer", "organizer@eventhub.local",
                            encoder.encode(organizerPassword), Role.ORGANIZER)));
            if (users.findByEmailIgnoreCase("admin@eventhub.local").isEmpty()) {
                users.save(new User("EventHub Admin", "admin@eventhub.local",
                        encoder.encode(adminPassword), Role.ADMIN));
            }
            if (events.count() == 0) {
                events.save(new Event("Delhi Sufi & Folk Night",
                        "An evening of Hindustani classics and contemporary Sufi vocals in the heart of Delhi.",
                        "Music", LocalDate.now().plusDays(21), LocalTime.of(19, 0), "Kamani Auditorium, New Delhi",
                        null, new BigDecimal("499.00"), 180, organizer));
                events.save(new Event("Old Delhi Street Food Walk",
                        "Discover Chandni Chowk's much-loved street food with a local guide.",
                        "Food", LocalDate.now().plusDays(28), LocalTime.NOON, "Chandni Chowk, New Delhi",
                        null, new BigDecimal("349.00"), 40, organizer));
                events.save(new Event("Jaipur Block Printing Workshop",
                        "Learn traditional hand block printing and take home your own printed fabric.",
                        "Arts", LocalDate.now().plusDays(35), LocalTime.of(14, 0),
                        "Anokhi Museum of Hand Printing, Jaipur", null, new BigDecimal("699.00"), 30, organizer));
            }
            updateDemoEvent(events, "City Lights Live", "Delhi Sufi & Folk Night",
                    "An evening of Hindustani classics and contemporary Sufi vocals in the heart of Delhi.",
                    "Music", 21, LocalTime.of(19, 0), "Kamani Auditorium, New Delhi", "499.00");
            updateDemoEvent(events, "The Weekend Table", "Old Delhi Street Food Walk",
                    "Discover Chandni Chowk's much-loved street food with a local guide.",
                    "Food", 28, LocalTime.NOON, "Chandni Chowk, New Delhi", "349.00");
            updateDemoEvent(events, "Ideas in Motion", "Jaipur Block Printing Workshop",
                    "Learn traditional hand block printing and take home your own printed fabric.",
                    "Arts", 35, LocalTime.of(14, 0), "Anokhi Museum of Hand Printing, Jaipur", "699.00");
            updateDemoPrice(events, "Mumbai Monsoon Music Night", "499.00");
            updateDemoPrice(events, "Jaipur Rangilo Folk Festival", "799.00");
            updateDemoPrice(events, "Dilli Street Food Walk", "299.00");
            updateDemoPrice(events, "Bengaluru Tech & Chai Meetup", "199.00");
            updateDemoPrice(events, "Kochi Onam Community Celebration", "299.00");
            addDemoEventIfMissing(events, organizer, "Ahmedabad Dandiya Raas Night",
                    "Celebrate Navratri with an energetic evening of Garba, Dandiya, and live folk music.",
                    "Music", 14, LocalTime.of(19, 0), "GMDC Ground, Ahmedabad", "399.00", 250);
            addDemoEventIfMissing(events, organizer, "Delhi Diwali Lights & Food Festival",
                    "Enjoy festive lights, handmade crafts, and classic Diwali treats with the community.",
                    "Community", 28, LocalTime.of(17, 0), "Dilli Haat, New Delhi", "499.00", 200);
            addDemoEventIfMissing(events, organizer, "Ahmedabad Makar Sankranti Kite Festival",
                    "Welcome Uttarayan with colorful kites, Gujarati snacks, and family-friendly activities.",
                    "Community", 90, LocalTime.of(9, 0), "Sabarmati Riverfront, Ahmedabad", "299.00", 150);
        };
    }

    private static void addDemoEventIfMissing(EventRepository events, User organizer, String name,
                                              String description, String category, int daysFromNow,
                                              LocalTime startTime, String venue, String price, int capacity) {
        if (events.findByNameIgnoreCase(name).isPresent()) return;

        events.save(new Event(name, description, category, LocalDate.now().plusDays(daysFromNow),
                startTime, venue, null, new BigDecimal(price), capacity, organizer));
    }

    private static void updateDemoEvent(EventRepository events, String oldName, String name,
                                        String description, String category, int daysFromNow,
                                        LocalTime startTime, String venue, String price) {
        Optional<Event> existing = events.findByNameIgnoreCase(oldName);
        if (existing.isEmpty()) return;

        Event event = existing.get();
        event.setName(name);
        event.setDescription(description);
        event.setCategory(category);
        event.setEventDate(LocalDate.now().plusDays(daysFromNow));
        event.setStartTime(startTime);
        event.setVenue(venue);
        event.setTicketPrice(new BigDecimal(price));
        events.save(event);
    }

    private static void updateDemoPrice(EventRepository events, String name, String price) {
        events.findByNameIgnoreCase(name).ifPresent(event -> {
            event.setTicketPrice(new BigDecimal(price));
            events.save(event);
        });
    }
}
