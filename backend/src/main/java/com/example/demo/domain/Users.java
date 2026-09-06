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
    private final Integer gradYear;
    private final List<String> classes, studying, preferredStudyLocations;
    private final List<Integer> studyTimes;

    public Users(ResultSet resultSet, List<String> classes, List<String> studying, List<Integer> studyTimes, List<String> locations) throws SQLException {
        this(resultSet.getLong("id"), resultSet.getString("username"), resultSet.getString("email"), resultSet.getString("bio"), resultSet.getString("comments"), resultSet.getString("picture_url"), resultSet.getString("avatar"), resultSet.getString("major"), numberOrNull(resultSet.getObject("grad_year")), resultSet.getString("created_at"), resultSet.getString("updated_at"), classes, studying, studyTimes, locations);
    }

    public Users(long id, String username, String email, String bio, String comments, String pictureUrl, String avatar, String major, Integer gradYear, String createdAt, String updatedAt, List<String> classes, List<String> studying, List<Integer> studyTimes, List<String> locations) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.bio = bio;
        this.comments = comments;
        this.pictureUrl = pictureUrl;
        this.avatar = avatar;
        this.major = major;
        this.gradYear = gradYear;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.classes = List.copyOf(classes);
        this.studying = List.copyOf(studying);
        this.studyTimes = List.copyOf(studyTimes);
        this.preferredStudyLocations = List.copyOf(locations);
    }

    private static Integer numberOrNull(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    public long id() {
        return id;
    }

    public List<String> studying() {
        return studying;
    }

    public List<Integer> studyTimes() {
        return studyTimes;
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
            studyTimes,
            preferredStudyLocations,
            createdAt,
            updatedAt);
    }

    public Map<String, Object> serialize(boolean includeEmail) {
        return profile(includeEmail).toMap();
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
        List<Integer> studyTimes,
        List<String> preferredStudyLocations,
        String createdAt,
        String updatedAt) {
        public Map<String, Object> toMap() {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", id);
            out.put("username", username);
            if (email != null) {
                out.put("email", email);
            }
            out.put("bio", bio);
            out.put("comments", comments);
            out.put("pictureUrl", pictureUrl);
            out.put("avatar", avatar);
            out.put("major", major);
            out.put("gradYear", gradYear);
            out.put("classes", classes);
            out.put("studying", studying);
            out.put("studyTimes", studyTimes);
            out.put("preferredStudyLocations", preferredStudyLocations);
            out.put("createdAt", createdAt);
            out.put("updatedAt", updatedAt);
            return out;
        }
    }
}
