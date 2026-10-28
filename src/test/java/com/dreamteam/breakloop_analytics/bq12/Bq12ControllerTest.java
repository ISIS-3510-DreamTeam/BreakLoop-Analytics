package com.dreamteam.breakloop_analytics.bq12;

import com.dreamteam.breakloop_analytics.controller.Bq12Controller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class Bq12ControllerTest {

    private MockMvc mvc;
    private SuggestionInteractionReader reader;

    @BeforeEach
    void setUp() {
        reader = mock(SuggestionInteractionReader.class);
        mvc = MockMvcBuilders.standaloneSetup(new Bq12Controller(new Bq12Service(reader))).build();
    }

    @Test
    void returnsAggregatedResultWithoutUidsOrCoordinates() throws Exception {
        long opened = Instant.parse("2026-10-20T15:00:00Z").toEpochMilli();
        when(reader.readAll()).thenReturn(List.of(
                new Interaction("secret-uid", opened, "AFTERNOON", "HOME",
                        List.of(new Interaction.Shown("a1", "READING")), "ACCEPTED", "a1", null)));

        mvc.perform(get("/api/analytics/bq12?from=2026-10-20&to=2026-10-20&minUsers=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minUsers").value(1))
                .andExpect(jsonPath("$.groups[0].category").value("READING"))
                .andExpect(jsonPath("$.groups[0].accepted").value(1))
                .andExpect(jsonPath("$.groups[0].acceptanceRate").value(1.0))
                .andExpect(jsonPath("$.suppressedGroups").value(0))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret-uid"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("latitude"))));
    }

    @Test
    void defaultMinUsersIsFiveAndSuppressesSmallGroups() throws Exception {
        long opened = Instant.parse("2026-10-20T15:00:00Z").toEpochMilli();
        when(reader.readAll()).thenReturn(List.of(
                new Interaction("u1", opened, "AFTERNOON", "HOME",
                        List.of(new Interaction.Shown("a1", "READING")), "NO_ACTIVITY", null, "NOT_NOW")));

        mvc.perform(get("/api/analytics/bq12?from=2026-10-20&to=2026-10-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minUsers").value(5))
                .andExpect(jsonPath("$.groups").isEmpty())
                .andExpect(jsonPath("$.suppressedGroups").value(1));
    }

    @Test
    void rejectsInvertedRange() throws Exception {
        mvc.perform(get("/api/analytics/bq12?from=2026-10-21&to=2026-10-20"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidMinUsers() throws Exception {
        mvc.perform(get("/api/analytics/bq12?minUsers=0"))
                .andExpect(status().isBadRequest());
    }
}
