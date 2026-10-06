package com.eventzone.backend.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password@123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private String firstEventId() throws Exception {
        String body = mockMvc.perform(get("/api/events")).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[0].id");
    }

    private String eventDetail(String eventId) throws Exception {
        return mockMvc.perform(get("/api/events/{id}", eventId)).andReturn().getResponse().getContentAsString();
    }

    private int availableSeats(String eventId, String ticketId) throws Exception {
        List<Integer> seats = JsonPath.read(eventDetail(eventId),
                "$.ticketCategories[?(@.id=='" + ticketId + "')].availableSeats");
        return seats.get(0);
    }

    private String bookingJson(String ticketId, int quantity) {
        return "{\"ticketCategoryId\":\"" + ticketId + "\",\"quantity\":" + quantity + "}";
    }

    @Test
    void bookThenCancelDecrementsAndRestoresAvailableSeats() throws Exception {
        String auth = login("user1@eventzone.com");
        String eventId = firstEventId();
        String ticketId = JsonPath.read(eventDetail(eventId), "$.ticketCategories[0].id");
        int before = availableSeats(eventId, ticketId);

        String created = mockMvc.perform(post("/api/bookings").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(bookingJson(ticketId, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingRef").value(org.hamcrest.Matchers.startsWith("EZ-")))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andReturn().getResponse().getContentAsString();
        String bookingId = JsonPath.read(created, "$.id");
        String bookingRef = JsonPath.read(created, "$.bookingRef");

        org.assertj.core.api.Assertions.assertThat(availableSeats(eventId, ticketId)).isEqualTo(before - 2);

        mockMvc.perform(get("/api/bookings/mine").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].bookingRef", hasItem(bookingRef)));

        mockMvc.perform(put("/api/bookings/{id}/cancel", bookingId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        org.assertj.core.api.Assertions.assertThat(availableSeats(eventId, ticketId)).isEqualTo(before);

        mockMvc.perform(put("/api/bookings/{id}/cancel", bookingId).header("Authorization", auth))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
        org.assertj.core.api.Assertions.assertThat(availableSeats(eventId, ticketId)).isEqualTo(before);
    }

    @Test
    void anotherUserCannotSeeOrCancelSomeoneElsesBooking() throws Exception {
        String owner = login("user1@eventzone.com");
        String other = login("org1@eventzone.com");
        String eventId = firstEventId();
        String ticketId = JsonPath.read(eventDetail(eventId), "$.ticketCategories[0].id");

        String created = mockMvc.perform(post("/api/bookings").header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON).content(bookingJson(ticketId, 1)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String bookingId = JsonPath.read(created, "$.id");
        String bookingRef = JsonPath.read(created, "$.bookingRef");

        mockMvc.perform(put("/api/bookings/{id}/cancel", bookingId).header("Authorization", other))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        mockMvc.perform(get("/api/bookings/mine").header("Authorization", other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].bookingRef", org.hamcrest.Matchers.not(hasItem(bookingRef))));

        mockMvc.perform(put("/api/bookings/{id}/cancel", bookingId).header("Authorization", owner))
                .andExpect(status().isOk());
    }

    @Test
    void quantityMustBeBetweenOneAndFive() throws Exception {
        String auth = login("user1@eventzone.com");
        String ticketId = JsonPath.read(eventDetail(firstEventId()), "$.ticketCategories[0].id");

        for (int quantity : new int[]{0, 6, -1}) {
            mockMvc.perform(post("/api/bookings").header("Authorization", auth)
                            .contentType(MediaType.APPLICATION_JSON).content(bookingJson(ticketId, quantity)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
        mockMvc.perform(post("/api/bookings").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownTicketCategoryIsNotFound() throws Exception {
        mockMvc.perform(post("/api/bookings").header("Authorization", login("user1@eventzone.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("00000000-0000-0000-0000-000000000000", 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookingEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson("00000000-0000-0000-0000-000000000000", 1)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/bookings/mine")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/bookings/{id}/cancel", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());
    }
}
