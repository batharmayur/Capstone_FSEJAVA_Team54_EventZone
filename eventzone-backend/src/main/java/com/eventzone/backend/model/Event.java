package com.eventzone.backend.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "event")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(nullable = false, length = 200)
    private String venue;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private EventCategory category;

    /** Owner of the event. Null only for events created before organisers were tracked. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organiser_id")
    private User organiser;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketCategory> ticketCategories = new ArrayList<>();

    protected Event() {
    }

    public Event(String title, String description, LocalDateTime eventDate, String venue,
                 String coverImageUrl, EventCategory category) {
        this.title = title;
        this.description = description;
        this.eventDate = eventDate;
        this.venue = venue;
        this.coverImageUrl = coverImageUrl;
        this.category = category;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public String getVenue() {
        return venue;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void update(String title, String description, LocalDateTime eventDate, String venue,
                       String coverImageUrl, EventCategory category) {
        this.title = title;
        this.description = description;
        this.eventDate = eventDate;
        this.venue = venue;
        this.coverImageUrl = coverImageUrl;
        this.category = category;
    }

    public User getOrganiser() {
        return organiser;
    }

    public void setOrganiser(User organiser) {
        this.organiser = organiser;
    }

    public EventCategory getCategory() {
        return category;
    }

    public List<TicketCategory> getTicketCategories() {
        return ticketCategories;
    }
}
