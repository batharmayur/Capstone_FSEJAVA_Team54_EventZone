package com.eventzone.backend.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.eventzone.backend.controller.TestApi.addTicket;
import static com.eventzone.backend.controller.TestApi.categoryId;
import static com.eventzone.backend.controller.TestApi.createEvent;
import static com.eventzone.backend.controller.TestApi.eventJson;
import static com.eventzone.backend.controller.TestApi.login;
import static com.eventzone.backend.controller.TestApi.uniqueTitle;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String admin() throws Exception {
        return login(mockMvc, "admin@eventzone.com");
    }

    private String createCategory(String auth, String name) throws Exception {
        String body = mockMvc.perform(post("/api/admin/categories").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void onlyAdminsCanAccessAdminEndpoints() throws Exception {
        for (String email : new String[]{"user1@eventzone.com", "org1@eventzone.com"}) {
            String auth = login(mockMvc, email);
            mockMvc.perform(get("/api/admin/events").header("Authorization", auth))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("FORBIDDEN"));
            mockMvc.perform(post("/api/admin/categories").header("Authorization", auth)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Nope\"}"))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/admin/events")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/events").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$[*].organiser", hasItem("Arjun Events")));
    }

    @Test
    void adminCategoryCrudMakesNewCategoryAvailableToOrganisers() throws Exception {
        String admin = admin();
        String name = uniqueTitle("Festival");
        String categoryId = createCategory(admin, name);

        mockMvc.perform(get("/api/categories")).andExpect(jsonPath("$[*].name", hasItem(name)));

        mockMvc.perform(post("/api/admin/categories").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + name.toUpperCase() + "\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CONFLICT"));
        mockMvc.perform(post("/api/admin/categories").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest());

        String renamed = name + " Renamed";
        mockMvc.perform(put("/api/admin/categories/{id}", categoryId).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + renamed + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value(renamed));

        String org = login(mockMvc, "org1@eventzone.com");
        String eventId = createEvent(mockMvc, org, uniqueTitle("Festival Event"), categoryId);

        mockMvc.perform(delete("/api/admin/categories/{id}", categoryId).header("Authorization", admin))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/events/{id}", eventId).header("Authorization", org)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/admin/categories/{id}", categoryId).header("Authorization", admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/categories")).andExpect(jsonPath("$[*].name", not(hasItem(renamed))));
        mockMvc.perform(delete("/api/admin/categories/{id}", categoryId).header("Authorization", admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanDeactivateAndReactivateAnEvent() throws Exception {
        String admin = admin();
        String org = login(mockMvc, "org1@eventzone.com");
        String attendee = login(mockMvc, "user1@eventzone.com");
        String title = uniqueTitle("Toggle Show");
        String eventId = createEvent(mockMvc, org, title, categoryId(mockMvc, "Concert"));
        String ticketId = addTicket(mockMvc, org, eventId, "General", "50", 10);
        String bookingJson = "{\"ticketCategoryId\":\"" + ticketId + "\",\"quantity\":1}";

        mockMvc.perform(put("/api/admin/events/{id}/active", eventId).header("Authorization", org)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/events/{id}/active", eventId).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/events")).andExpect(jsonPath("$[*].title", not(hasItem(title))));
        mockMvc.perform(get("/api/events/{id}", eventId)).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/bookings").header("Authorization", attendee)
                        .contentType(MediaType.APPLICATION_JSON).content(bookingJson))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/organiser/events").header("Authorization", org))
                .andExpect(jsonPath("$[?(@.id=='" + eventId + "')].active", hasItem(false)));

        mockMvc.perform(put("/api/admin/events/{id}/active", eventId).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/events")).andExpect(jsonPath("$[*].title", hasItem(title)));
        String booking = mockMvc.perform(post("/api/bookings").header("Authorization", attendee)
                        .contentType(MediaType.APPLICATION_JSON).content(bookingJson))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        mockMvc.perform(put("/api/bookings/{id}/cancel", JsonPath.<String>read(booking, "$.id"))
                .header("Authorization", attendee)).andExpect(status().isOk());

        mockMvc.perform(put("/api/admin/events/{id}/active", eventId).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/admin/events/{id}/active", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void organiserEventFormRejectsAnUnknownCategoryAfterDeletion() throws Exception {
        String admin = admin();
        String categoryId = createCategory(admin, uniqueTitle("Temp"));
        mockMvc.perform(delete("/api/admin/categories/{id}", categoryId).header("Authorization", admin))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/events").header("Authorization", login(mockMvc, "org1@eventzone.com"))
                        .contentType(MediaType.APPLICATION_JSON).content(eventJson("Orphan", categoryId, 30)))
                .andExpect(status().isNotFound());
    }
}
