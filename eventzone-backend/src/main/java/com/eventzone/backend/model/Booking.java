package com.eventzone.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_category_id", nullable = false)
    private TicketCategory ticketCategory;

    @Column(nullable = false)
    private int quantity;

    /** Price at the time of booking (unit price x quantity), unaffected by later price changes. */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 20)
    private String status = BookingStatus.CONFIRMED;

    @Column(name = "booking_ref", nullable = false, unique = true, length = 20)
    private String bookingRef;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Booking() {
    }

    public Booking(User user, TicketCategory ticketCategory, int quantity, BigDecimal totalAmount, String bookingRef) {
        this.user = user;
        this.ticketCategory = ticketCategory;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.bookingRef = bookingRef;
    }

    public void cancel() {
        this.status = BookingStatus.CANCELLED;
    }

    public boolean isCancelled() {
        return BookingStatus.CANCELLED.equals(status);
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public TicketCategory getTicketCategory() {
        return ticketCategory;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public String getBookingRef() {
        return bookingRef;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
