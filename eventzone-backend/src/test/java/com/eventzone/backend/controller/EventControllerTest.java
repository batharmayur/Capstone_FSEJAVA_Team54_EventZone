package com.eventzone.backend.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsAtLeastFiveSeededEvents() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$[0].title").isNotEmpty())
                .andExpect(jsonPath("$[0].category").isNotEmpty())
                .andExpect(jsonPath("$[0].eventDate").isNotEmpty())
                .andExpect(jsonPath("$[0].coverImageUrl").isNotEmpty());
    }

    @Test
    void filtersEventsByCategory() throws Exception {
        mockMvc.perform(get("/api/events").param("category", "Sports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].category", everyItem(is("Sports"))));
    }

    @Test
    void returnsEventDetailWithTicketCategories() throws Exception {
        String body = mockMvc.perform(get("/api/events")).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$[0].id");

        mockMvc.perform(get("/api/events/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.description").isNotEmpty())
                .andExpect(jsonPath("$.ticketCategories.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.ticketCategories[0].availableSeats").isNumber());
    }

    @Test
    void unknownEventReturnsStructuredNotFound() throws Exception {
        String id = "00000000-0000-0000-0000-000000000000";

        mockMvc.perform(get("/api/events/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/events/" + id))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void malformedEventIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/events/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void listsCategories() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(3)));
    }
}
