package com.eventzone.backend.repository;

import com.eventzone.backend.model.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    boolean existsByTicketCategoryId(UUID ticketCategoryId);

    boolean existsByTicketCategoryEventId(UUID eventId);

    /** Rows of [ticketCategoryId, number of confirmed bookings]; categories without bookings are omitted. */
    @Query("select b.ticketCategory.id, count(b) from Booking b "
            + "where b.status = 'CONFIRMED' and b.ticketCategory.id in :ids group by b.ticketCategory.id")
    List<Object[]> countConfirmedByTicketCategoryIds(@Param("ids") Collection<UUID> ids);

    @EntityGraph(attributePaths = {"ticketCategory", "ticketCategory.event"})
    List<Booking> findByUserEmailOrderByCreatedAtDesc(String email);

    /** Locks the row so two simultaneous cancellations cannot both restore the seats. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b join fetch b.ticketCategory t join fetch t.event "
            + "where b.id = :id and b.user.email = :email")
    Optional<Booking> findForUpdateByIdAndUserEmail(@Param("id") UUID id, @Param("email") String email);
}
