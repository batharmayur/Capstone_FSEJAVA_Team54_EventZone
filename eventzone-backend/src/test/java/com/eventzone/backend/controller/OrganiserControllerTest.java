package com.eventzone.backend.controller;

import com.eventzone.backend.model.Role;
import com.eventzone.backend.model.User;
import com.eventzone.backend.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.eventzone.backend.controller.TestApi.addTicket;
import static com.eventzone.backend.controller.TestApi.categoryId;
import static com.eventzone.backend.controller.TestApi.createEvent;
import static com.eventzone.backend.controller.TestApi.eventJson;
import static com.eventzone.backend.controller.TestApi.login;
import static com.eventzone.backend.controller.TestApi.ticketJson;
import static com.eventzone.backend.controller.TestApi.uniqueTitle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrganiserControllerTest {

    private static final String ORG1 = "org1@eventzone.com";
    private static final String ORG2 = "org2.test@eventzone.com";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String organiser() throws Exception {
        return login(mockMvc, ORG1);
    }

    private String secondOrganiser() throws Exception {
        if (!userRepository.existsByEmail(ORG2)) {
            userRepository.save(new User(ORG2, passwordEncoder.encode(TestApi.PASSWORD), Role.ORGANISER, "Second"));
        }
        return login(mockMvc, ORG2);
    }

    @Test
    void organiserCreatesEventThatAppearsInTheCatalogImmediately() throws Exception {
        String auth = organiser();
        String title = uniqueTitle("Catalog Show");
        String eventId = createEvent(mockMvc, auth, title, categoryId(mockMvc, "Workshop"));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem(title)));

        mockMvc.perform(get("/api/organiser/events").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(eventId)));

        mockMvc.perform(delete("/api/events/{id}", eventId).header("Authorization", auth))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/events/{id}", eventId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/events")).andExpect(jsonPath("$[*].title", not(hasItem(title))));
    }

    @Test
    void organiserManagesTicketCategoriesAndSeesBookingsPerCategory() throws Exception {
        String org = organiser();
        String eventId = createEvent(mockMvc, org, uniqueTitle("Ticket Show"), categoryId(mockMvc, "Concert"));
        String ticketId = addTicket(mockMvc, org, eventId, "General", "499.50", 20);

        mockMvc.perform(get("/api/events/{id}", eventId))
                .andExpect(jsonPath("$.ticketCategories[0].name").value("General"))
                .andExpect(jsonPath("$.ticketCategories[0].availableSeats").value(20));

        String attendee = login(mockMvc, "user1@eventzone.com");
        String booking = mockMvc.perform(post("/api/bookings").header("Authorization", attendee)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketCategoryId\":\"" + ticketId + "\",\"quantity\":3}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String bookingId = JsonPath.read(booking, "$.id");

        String mine = mockMvc.perform(get("/api/organiser/events").header("Authorization", org))
                .andReturn().getResponse().getContentAsString();
        List<Integer> booked = JsonPath.read(mine, "$[?(@.id=='" + eventId + "')].ticketCategories[0].bookedSeats");
        List<Integer> count = JsonPath.read(mine, "$[?(@.id=='" + eventId + "')].ticketCategories[0].bookingCount");
        assertThat(booked).containsExactly(3);
        assertThat(count).containsExactly(1);

        mockMvc.perform(put("/api/ticket-categories/{id}", ticketId).header("Authorization", org)
                        .contentType(MediaType.APPLICATION_JSON).content(ticketJson("General", "499.50", 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("3 seat(s) already booked")));

        mockMvc.perform(put("/api/ticket-categories/{id}", ticketId).header("Authorization", org)
                        .contentType(MediaType.APPLICATION_JSON).content(ticketJson("General Admission", "550", 30)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("General Admission"))
                .andExpect(jsonPath("$.totalSeats").value(30))
                .andExpect(jsonPath("$.availableSeats").value(27))
                .andExpect(jsonPath("$.bookedSeats").value(3))
                .andExpect(jsonPath("$.bookingCount").value(1));

        mockMvc.perform(delete("/api/ticket-categories/{id}", ticketId).header("Authorization", org))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/events/{id}", eventId).header("Authorization", org))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/bookings/{id}/cancel", bookingId).header("Authorization", attendee))
                .andExpect(status().isOk());
    }

    @Test
    void organiserCanEditAndDeleteAnUnbookedTicketCategory() throws Exception {
        String org = organiser();
        String eventId = createEvent(mockMvc, org, uniqueTitle("Edit Show"), categoryId(mockMvc, "Workshop"));
        String ticketId = addTicket(mockMvc, org, eventId, "Early Bird", "100", 5);

        mockMvc.perform(put("/api/events/{id}", eventId).header("Authorization", org)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("Renamed Show", categoryId(mockMvc, "Sports"), 700)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed Show"))
                .andExpect(jsonPath("$.category").value("Sports"))
                .andExpect(jsonPath("$.ticketCategories.length()").value(1));

        mockMvc.perform(delete("/api/ticket-categories/{id}", ticketId).header("Authorization", org))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/events/{id}", eventId))
                .andExpect(jsonPath("$.ticketCategories.length()").value(0));

        mockMvc.perform(delete("/api/events/{id}", eventId).header("Authorization", org))
                .andExpect(status().isNoContent());
    }

    @Test
    void attendeesAndGuestsCannotUseOrganiserEndpoints() throws Exception {
        String attendee = login(mockMvc, "user1@eventzone.com");
        String category = categoryId(mockMvc, "Concert");

        mockMvc.perform(post("/api/events").header("Authorization", attendee)
                        .contentType(MediaType.APPLICATION_JSON).content(eventJson("Nope", category, 30)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mockMvc.perform(get("/api/organiser/events").header("Authorization", attendee))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/events/{id}", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", attendee))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("Nope", category, 30)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void organiserCannotManageSomeoneElsesEventButAdminCan() throws Exception {
        String owner = organiser();
        String other = secondOrganiser();
        String admin = login(mockMvc, "admin@eventzone.com");
        String category = categoryId(mockMvc, "Workshop");
        String eventId = createEvent(mockMvc, owner, uniqueTitle("Owned Show"), category);
        String ticketId = addTicket(mockMvc, owner, eventId, "General", "10", 10);

        mockMvc.perform(put("/api/events/{id}", eventId).header("Authorization", other)
                        .contentType(MediaType.APPLICATION_JSON).content(eventJson("Hijack", category, 40)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/events/{id}", eventId).header("Authorization", other))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/events/{id}/ticket-categories", eventId).header("Authorization", other)
                        .contentType(MediaType.APPLICATION_JSON).content(ticketJson("Sneaky", "1", 1)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/ticket-categories/{id}", ticketId).header("Authorization", other)
                        .contentType(MediaType.APPLICATION_JSON).content(ticketJson("General", "10", 999)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/organiser/events").header("Authorization", other))
                .andExpect(jsonPath("$[*].id", not(hasItem(eventId))));

        mockMvc.perform(put("/api/events/{id}", eventId).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(eventJson("Admin Edited", category, 40)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Admin Edited"));
        mockMvc.perform(post("/api/events").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(eventJson("Admin Create", category, 40)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/events/{id}", eventId).header("Authorization", admin))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidEventAndTicketInputIsRejected() throws Exception {
        String org = organiser();
        String category = categoryId(mockMvc, "Concert");
        String eventId = createEvent(mockMvc, org, uniqueTitle("Valid Show"), category);

        mockMvc.perform(post("/api/events").header("Authorization", org).contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("Past Show", category, -1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/events").header("Authorization", org).contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("  ", category, 30)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/events").header("Authorization", org).contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("Bad Image", category, 30).replace("/images/events/placeholder.svg", "javascript:alert(1)")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/events").header("Authorization", org).contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("No Category", "00000000-0000-0000-0000-000000000000", 30)))
                .andExpect(status().isNotFound());

        for (String bad : new String[]{ticketJson("Free?", "-1", 5), ticketJson("", "10", 5),
                ticketJson("Zero", "10", 0), ticketJson("TooMany", "10", 100001), ticketJson("Cents", "1.234", 5)}) {
            mockMvc.perform(post("/api/events/{id}/ticket-categories", eventId).header("Authorization", org)
                            .contentType(MediaType.APPLICATION_JSON).content(bad))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(delete("/api/events/{id}", eventId).header("Authorization", org))
                .andExpect(status().isNoContent());
    }
}
