package com.eventzone.backend.controller;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Helpers shared by the end-to-end controller tests. */
final class TestApi {

    static final String PASSWORD = "Password@123";

    private TestApi() {
    }

    static String login(MockMvc mvc, String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    static String categoryId(MockMvc mvc, String name) throws Exception {
        String body = mvc.perform(get("/api/categories")).andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(body, "$[?(@.name=='" + name + "')].id");
        return ids.get(0);
    }

    static String eventJson(String title, String categoryId, int daysAhead) {
        return "{\"title\":\"" + title + "\",\"description\":\"A test event\",\"eventDate\":\""
                + LocalDateTime.now().plusDays(daysAhead).withNano(0) + "\",\"venue\":\"Test Arena\","
                + "\"coverImageUrl\":\"/images/events/placeholder.svg\",\"categoryId\":\"" + categoryId + "\"}";
    }

    static String ticketJson(String name, String price, int totalSeats) {
        return "{\"name\":\"" + name + "\",\"price\":" + price + ",\"totalSeats\":" + totalSeats + "}";
    }

    static String uniqueTitle(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Creates an event as the given organiser and returns its id. */
    static String createEvent(MockMvc mvc, String auth, String title, String categoryId) throws Exception {
        String body = mvc.perform(post("/api/events").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(eventJson(title, categoryId, 600)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    /** Adds a ticket category and returns its id. */
    static String addTicket(MockMvc mvc, String auth, String eventId, String name, String price, int seats)
            throws Exception {
        String body = mvc.perform(post("/api/events/{id}/ticket-categories", eventId).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(ticketJson(name, price, seats)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}
