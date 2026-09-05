package com.example.demo.domain;

import com.example.demo.api.ApiException;
import org.springframework.http.HttpStatus;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Owns the JSON cache representation so services only read/write a domain value.
 */
public record LookingNow(long userId, List<String> subjects, String expiresAt) {
    public LookingNow {
        subjects = List.copyOf(subjects);
    }

    public static LookingNow deserialize(long userId, String subjectsJson, String expiresAt, ObjectMapper json) {
        try {
            List<String> values = json.readValue(subjectsJson, new TypeReference<List<String>>() {
            });
            return new LookingNow(userId, validate(values), expiresAt);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "invalid_presence", "Stored looking-now presence is invalid");
        }
    }

    public static List<String> validate(List<?> values) {
        if (values == null || values.isEmpty() || values.size() > 20)
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_subjects", "subjects must be a non-empty array");
        LinkedHashSet<String> clean = new LinkedHashSet<>();
        for (Object value : values) {
            if (!(value instanceof String s) || s.trim().isEmpty() || s.trim().length() > 100)
                throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_subjects", "subjects must contain non-empty strings");
            clean.add(s.trim());
        }
        return List.copyOf(clean);
    }

    public String toSubjectsJson(ObjectMapper json) {
        try {
            return json.writeValueAsString(subjects);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize subjects", e);
        }
    }

    public String serializeSubjects(ObjectMapper json) {
        return toSubjectsJson(json);
    }
}
