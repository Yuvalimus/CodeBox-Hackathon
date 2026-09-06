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
        assertEquals(.70 * .5 + .15 + .15, RecommendationService.score(a, b), .00001);
        assertEquals(Set.of("CSC101", "CSC202"), a.studying());
    }
}
