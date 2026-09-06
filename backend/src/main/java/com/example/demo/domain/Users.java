package com.example.demo.domain;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregate returned by user queries. Password hashes are deliberately not part of this type.
 */
public final class Users {
    private final long id;
    private final String username, email, bio, comments, pictureUrl, avatar, major, createdAt, updatedAt;
    private final Integer gradYear, studyDurationMinutes;
    private final boolean offlineDiscoverable;
    private final List<String> classes, studying, preferredStudyLocations;

    public Users(ResultSet resultSet, List<String> classes, List<String> studying, List<String> locations) throws SQLException {
        this(resultSet.getLong("id"), resultSet.getString("username"), resultSet.getString("email"), resultSet.getString("bio"), resultSet.getString("comments"), resultSet.getString("picture_url"), resultSet.getString("avatar"), resultSet.getString("major"), nullableInteger(resultSet, "grad_year"), resultSet.getInt("study_duration_minutes"), resultSet.getBoolean("offline_discoverable"), resultSet.getString("created_at"), resultSet.getString("updated_at"), classes, studying, locations);
    }

    public Users(long id, String username, String email, String bio, String comments, String pictureUrl, String avatar, String major, Integer gradYear, Integer studyDurationMinutes, boolean offlineDiscoverable, String createdAt, String updatedAt, List<String> classes, List<String> studying, List<String> locations) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.bio = bio;
        this.comments = comments;
        this.pictureUrl = pictureUrl;
        this.avatar = avatar;
        this.major = major;
        this.gradYear = gradYear;
        this.studyDurationMinutes = studyDurationMinutes;
        this.offlineDiscoverable = offlineDiscoverable;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.classes = List.copyOf(classes);
        this.studying = List.copyOf(studying);
        this.preferredStudyLocations = List.copyOf(locations);
    }

    private static Integer nullableInteger(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    public long id() {
        return id;
    }

    public List<String> classes() {
        return classes;
    }

    public List<String> studying() {
        return studying;
    }

    public Integer studyDurationMinutes() {
        return studyDurationMinutes;
    }

    public Integer gradYear() {
        return gradYear;
    }

    public Profile profile(boolean includeEmail) {
        return new Profile(
            id,
            username,
            includeEmail ? email : null,
            bio,
            comments,
            pictureUrl,
            avatar,
            major,
            gradYear,
            classes,
            studying,
            studyDurationMinutes,
            offlineDiscoverable,
            preferredStudyLocations,
            createdAt,
            updatedAt);
    }

    public PublicProfile publicProfile() {
        return new PublicProfile(id, username, bio, comments, pictureUrl, avatar, major, gradYear,
            classes, studying, studyDurationMinutes, offlineDiscoverable, preferredStudyLocations, createdAt, updatedAt);
    }

    /**
     * Transitional adapter for endpoints that still need to omit null email fields.
     * New API responses should prefer their dedicated typed response records.
     */
    public Map<String, Object> serialize(boolean includeEmail) {
        Profile profile = profile(includeEmail);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", profile.id());
        values.put("username", profile.username());
        if (profile.email() != null) values.put("email", profile.email());
        values.put("bio", profile.bio());
        values.put("comments", profile.comments());
        values.put("pictureUrl", profile.pictureUrl());
        values.put("avatar", profile.avatar());
        values.put("major", profile.major());
        values.put("gradYear", profile.gradYear());
        values.put("classes", profile.classes());
        values.put("studying", profile.studying());
        values.put("studyDurationMinutes", profile.studyDurationMinutes());
        values.put("offlineDiscoverable", profile.offlineDiscoverable());
        values.put("preferredStudyLocations", profile.preferredStudyLocations());
        values.put("createdAt", profile.createdAt());
        values.put("updatedAt", profile.updatedAt());
        return values;
    }

    public record Profile(
        long id,
        String username,
        String email,
        String bio,
        String comments,
        String pictureUrl,
        String avatar,
        String major,
        Integer gradYear,
        List<String> classes,
        List<String> studying,
        Integer studyDurationMinutes,
        boolean offlineDiscoverable,
        List<String> preferredStudyLocations,
        String createdAt,
        String updatedAt) { }

    /** A profile suitable for another user: it deliberately excludes the email address. */
    public record PublicProfile(
        long id,
        String username,
        String bio,
        String comments,
        String pictureUrl,
        String avatar,
        String major,
        Integer gradYear,
        List<String> classes,
        List<String> studying,
        Integer studyDurationMinutes,
        boolean offlineDiscoverable,
        List<String> preferredStudyLocations,
        String createdAt,
        String updatedAt) { }
}
