package com.eventzone.backend.config;

import com.eventzone.backend.model.Event;
import com.eventzone.backend.model.EventCategory;
import com.eventzone.backend.model.Role;
import com.eventzone.backend.model.TicketCategory;
import com.eventzone.backend.model.User;
import com.eventzone.backend.repository.EventCategoryRepository;
import com.eventzone.backend.repository.EventRepository;
import com.eventzone.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Seeds sample categories, events and ticket categories on first start (empty database only).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    static final String DEMO_PASSWORD = "Password@123";

    private final EventCategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(EventCategoryRepository categoryRepository, EventRepository eventRepository,
                      UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedEvents();
    }

    /** Demo accounts for local development only; all share {@value #DEMO_PASSWORD}. */
    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }
        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        userRepository.save(new User("admin@eventzone.com", hash, Role.ADMIN, "Admin"));
        userRepository.save(new User("org1@eventzone.com", hash, Role.ORGANISER, "Arjun Events"));
        userRepository.save(new User("user1@eventzone.com", hash, Role.ATTENDEE, "Divya"));
    }

    private void seedEvents() {
        if (categoryRepository.count() > 0) {
            return;
        }

        EventCategory concert = categoryRepository.save(new EventCategory("Concert"));
        EventCategory sports = categoryRepository.save(new EventCategory("Sports"));
        EventCategory workshop = categoryRepository.save(new EventCategory("Workshop"));
        EventCategory conference = categoryRepository.save(new EventCategory("Conference"));

        seed("Rock Night", "Live rock concert by top bands.", 20, 19, "HICC Hyderabad",
                "/images/events/concert-rock.svg", concert, "General:999:200", "VIP:2499:50");
        seed("Indie Sunset Sessions", "An evening of acoustic and indie acts under the open sky.", 35, 18,
                "Shilpakala Vedika, Hyderabad", "/images/events/concert-indie.svg", concert,
                "General:599:300", "Premium:1499:80");
        seed("Championship Weekend", "Finals of the city football championship.", 14, 16,
                "Gachibowli Stadium, Hyderabad", "/images/events/sports-football.svg", sports,
                "Stands:350:1000", "Box:1800:60");
        seed("City Marathon", "10K and half marathon through the heart of the city.", 45, 6,
                "Necklace Road, Hyderabad", "/images/events/sports-marathon.svg", sports,
                "10K:500:500", "Half Marathon:800:300");
        seed("Spring Boot Hands-on Workshop", "A full-day workshop building REST APIs with Spring Boot.", 25, 10,
                "T-Hub, Hyderabad", "/images/events/workshop.svg", workshop, "Standard:1500:40");
        seed("AI Product Meetup", "Talks and demos on building products with generative AI.", 30, 17,
                "Microsoft Campus, Hyderabad", "/images/events/conference.svg", conference,
                "Standard:0:150", "Networking Pass:999:50");
    }

    private void seed(String title, String description, int daysFromNow, int startHour, String venue,
                      String imageUrl, EventCategory category, String... tickets) {
        LocalDateTime date = LocalDateTime.of(LocalDateTime.now().toLocalDate().plusDays(daysFromNow),
                LocalTime.of(startHour, 0));
        Event seeded = new Event(title, description, date, venue, imageUrl, category);
        seeded.setOrganiser(userRepository.findByEmail("org1@eventzone.com").orElse(null));
        Event event = eventRepository.save(seeded);

        for (String ticket : tickets) {
            String[] parts = ticket.split(":");
            event.getTicketCategories().add(
                    new TicketCategory(event, parts[0], new BigDecimal(parts[1]), Integer.parseInt(parts[2])));
        }
        eventRepository.save(event);
    }
}
