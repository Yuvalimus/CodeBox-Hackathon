package com.example.demo;

import com.example.demo.service.RecommendationService;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecommendationServiceTests {
    @Test
    public void scorerWeightsStudyOverlapAndDoesNotMutateInputs() {
        RecommendationService.CompatibilityProfile a = new RecommendationService.CompatibilityProfile(Set.of("CSC101", "CSC202"), Set.of(1, 2), 2027);
        RecommendationService.CompatibilityProfile b = new RecommendationService.CompatibilityProfile(Set.of("CSC101"), Set.of(2), 2027);
        // CSC overlap is .5; the 15-minute-near time has partial credit.
        assertEquals(.70 * .5 + .15 * .984375 + .15, RecommendationService.score(a, b), .00001);
        assertEquals(Set.of("CSC101", "CSC202"), a.studying());
    }

    @Test
    public void scorerRewardsNearbyStudyTimesWithoutRequiringAnExactMatch() {
        RecommendationService.CompatibilityProfile first = new RecommendationService.CompatibilityProfile(
            Set.of("CSC101"), Set.of(100), 2027);
        RecommendationService.CompatibilityProfile nearby = new RecommendationService.CompatibilityProfile(
            Set.of("CSC101"), Set.of(104), 2027);
        RecommendationService.CompatibilityProfile distant = new RecommendationService.CompatibilityProfile(
            Set.of("CSC101"), Set.of(120), 2027);

        assertEquals(.70 + .15 * .75 + .15, RecommendationService.score(first, nearby), .00001);
        assertEquals(.70 + .15, RecommendationService.score(first, distant), .00001);
    }
}
