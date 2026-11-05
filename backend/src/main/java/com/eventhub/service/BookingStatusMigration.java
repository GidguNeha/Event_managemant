package com.eventhub.service;

import com.eventhub.model.BookingStatus;
import com.eventhub.repository.BookingRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BookingStatusMigration implements ApplicationRunner {
    private final BookingRepository bookings;
    private final JdbcTemplate jdbc;

    public BookingStatusMigration(BookingRepository bookings, JdbcTemplate jdbc) {
        this.bookings = bookings;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        String databaseProduct = jdbc.execute((ConnectionCallback<String>) connection ->
                connection.getMetaData().getDatabaseProductName());
        if ("H2".equalsIgnoreCase(databaseProduct)) {
            jdbc.execute("ALTER TABLE bookings ALTER COLUMN booking_status SET DATA TYPE VARCHAR(20)");
        }
        bookings.confirmPendingBookings(BookingStatus.PENDING, BookingStatus.CONFIRMED);
    }
}
