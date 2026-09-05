package com.example.demo;

import com.example.demo.service.RecommendationService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecommendationServiceTests {
    @Test
    public void scorerWeightsStudyOverlapAndDoesNotMutateInputs() {
        Map<String, Object> a = new HashMap<>(Map.of("studying", List.of("CSC101", "CSC202"), "studyTimes", List.of(1, 2), "gradYear", 2027));
        Map<String, Object> b = new HashMap<>(Map.of("studying", List.of("CSC101"), "studyTimes", List.of(2), "gradYear", 2027));
        assertEquals(.70 * .5 + .15 + .15, RecommendationService.score(a, b), .00001);
        assertEquals(List.of("CSC101", "CSC202"), a.get("studying"));
    }
}
