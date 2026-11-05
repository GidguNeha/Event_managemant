package com.eventhub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.eventhub.model.Booking;
import com.eventhub.model.BookingStatus;

import jakarta.persistence.LockModeType;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);
    List<Booking> findByUserIdOrderByBookedAtDesc(Long userId);
    List<Booking> findByEventOrganizerIdOrderByBookedAtDesc(Long organizerId);
    Optional<Booking> findByIdAndUserId(Long id, Long userId);
    long countByBookingStatus(BookingStatus status);
    long countByEventOrganizerId(Long organizerId);
    boolean existsByEventId(Long eventId);
    List<Booking> findByEventIdOrderByBookedAtDesc(Long eventId);
    @Query("select coalesce(sum(b.ticketQuantity), 0) from Booking b where b.event.id = :eventId and b.user.id = :userId and b.bookingStatus = :status")
    long sumTicketQuantityForUserAndEvent(@Param("eventId") Long eventId, @Param("userId") Long userId,
                                          @Param("status") BookingStatus status);
    @Query("select coalesce(sum(b.ticketQuantity), 0) from Booking b where b.event.id = :eventId and b.user.id = :userId and b.bookingStatus in :statuses")
    long sumTicketQuantityForUserAndEventInStatuses(@Param("eventId") Long eventId, @Param("userId") Long userId,
                                                     @Param("statuses") List<BookingStatus> statuses);
    @Modifying
    @Transactional
    @Query("update Booking b set b.bookingStatus = :confirmed where b.bookingStatus = :pending")
    int confirmPendingBookings(@Param("pending") BookingStatus pending,
                               @Param("confirmed") BookingStatus confirmed);
}
