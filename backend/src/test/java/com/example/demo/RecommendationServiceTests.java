package com.example.demo;

import com.example.demo.service.RecommendationService;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecommendationServiceTests {
    @Test
    public void scorerWeightsStudyOverlapAndDoesNotMutateInputs() {
        RecommendationService.CompatibilityProfile a = new RecommendationService.CompatibilityProfile(Set.of("CSC101", "CSC202"), 60, 2027);
        RecommendationService.CompatibilityProfile b = new RecommendationService.CompatibilityProfile(Set.of("CSC101"), 60, 2027);
        assertEquals(.70 * .5 + .15 + .15, RecommendationService.score(a, b), .00001);
        assertEquals(Set.of("CSC101", "CSC202"), a.studying());
    }

    @Test
    public void scorerRewardsSimilarStudyDurations() {
        RecommendationService.CompatibilityProfile first = new RecommendationService.CompatibilityProfile(
            Set.of("CSC101"), 60, 2027);
        RecommendationService.CompatibilityProfile similar = new RecommendationService.CompatibilityProfile(
            Set.of("CSC101"), 90, 2027);
        RecommendationService.CompatibilityProfile distant = new RecommendationService.CompatibilityProfile(
            Set.of("CSC101"), 120, 2027);

        assertEquals(.70 + .15 * (2.0 / 3.0) + .15, RecommendationService.score(first, similar), .00001);
        assertEquals(.70 + .15 * .5 + .15, RecommendationService.score(first, distant), .00001);
    }
}
