package com.example.demo.service;

import com.example.demo.api.ApiException;
import com.example.demo.domain.Users;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
public class UserService {
    private static final Set<String> RESERVED_USERNAMES = Set.of("admin", "support", "me");
    private static final Set<String> PROFILE_ARRAY_FIELDS = Set.of("classes", "studying", "studyTimes", "preferredStudyLocations");
    private static final Duration PROFILE_CACHE_TTL = Duration.ofMinutes(15);
    private static final String PROFILE_CACHE_PREFIX = "profile:";
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ReadCache readCache;

    public UserService(JdbcTemplate jdbcTemplate, ReadCache readCache) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.readCache = readCache;
    }

    public static String requiredText(Map<String, Object> requestBody, String fieldName, int minimumLength, int maximumLength) {
        Object rawValue = requestBody.get(fieldName);
        if (!(rawValue instanceof String)) throwInvalid(fieldName, fieldName + " is invalid");
        String value = ((String) rawValue).trim();
        if (value.length() < minimumLength || value.length() > maximumLength)
            throwInvalid(fieldName, fieldName + " is invalid");
        return value;
    }

    public static List<String> stringArray(Object rawValue, String fieldName, int maximumItems, int maximumItemLength) {
        if (rawValue == null) return List.of();
        if (!(rawValue instanceof List<?>)) throwInvalid(fieldName, fieldName + " must be an array");
        List<?> rawItems = (List<?>) rawValue;
        if (rawItems.size() > maximumItems) throwInvalid(fieldName, fieldName + " must be an array");
        LinkedHashSet<String> uniqueItems = new LinkedHashSet<>();
        for (Object rawItem : rawItems) {
            if (!(rawItem instanceof String)) throwInvalid(fieldName, fieldName + " has an invalid item");
            String item = ((String) rawItem).trim();
            if (item.isEmpty() || item.length() > maximumItemLength)
                throwInvalid(fieldName, fieldName + " has an invalid item");
            uniqueItems.add(item);
        }
        return List.copyOf(uniqueItems);
    }

    private static String optionalText(Map<String, Object> requestBody, String fieldName, int maximumLength) {
        Object rawValue = requestBody.get(fieldName);
        if (rawValue == null) return "";
        if (!(rawValue instanceof String)) throwInvalid(fieldName, fieldName + " is invalid");
        String value = ((String) rawValue).trim();
        if (value.length() > maximumLength) throwInvalid(fieldName, fieldName + " is invalid");
        return value;
    }

    private static List<Integer> studyTimes(Object rawValue) {
        if (rawValue == null) return List.of();
        if (!(rawValue instanceof List<?>)) throwInvalid("studyTimes", "studyTimes must be an array");
        List<?> rawHours = (List<?>) rawValue;
        if (rawHours.size() > 168) throwInvalid("studyTimes", "studyTimes must be an array");
        Set<Integer> uniqueHours = new TreeSet<>();
        for (Object rawHour : rawHours) {
            if (!(rawHour instanceof Number)) throwInvalid("studyTimes", "studyTimes must be hours from 0 through 167");
            Number hour = (Number) rawHour;
            if (hour.intValue() < 0 || hour.intValue() > 167 || hour.doubleValue() != hour.intValue())
                throwInvalid("studyTimes", "studyTimes must be hours from 0 through 167");
            uniqueHours.add(hour.intValue());
        }
        return List.copyOf(uniqueHours);
    }

    private static Integer graduationYear(Object rawValue) {
        if (rawValue == null) return null;
        if (!(rawValue instanceof Number)) throwInvalid("gradYear", "gradYear is invalid");
        Number year = (Number) rawValue;
        if (year.intValue() < 2020 || year.intValue() > 2100 || year.doubleValue() != year.intValue())
            throwInvalid("gradYear", "gradYear is invalid");
        return year.intValue();
    }

    private static String pictureUrl(Object rawValue) {
        if (rawValue == null) return null;
        if (!(rawValue instanceof String)) throwInvalid("pictureUrl", "pictureUrl must be HTTPS");
        String url = ((String) rawValue).trim();
        if (!url.matches("https://.+")) throwInvalid("pictureUrl", "pictureUrl must be HTTPS");
        return url;
    }

    private static void throwInvalid(String fieldName, String message) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_" + fieldName, message);
    }

    @Transactional
    public long register(Map<String, Object> requestBody) {
        String username = requiredText(requestBody, "username", 3, 32).toLowerCase(Locale.ROOT);
        String email = requiredText(requestBody, "email", 6, 254).toLowerCase(Locale.ROOT);
        String password = requiredText(requestBody, "password", 12, 200);
        validateRegistration(username, email);
        String timestamp = Instant.now().toString();
        try {
            jdbcTemplate.update("INSERT INTO users(username,email,password_hash,bio,picture_url,major,grad_year,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?)", username, email, passwordEncoder.encode(password), optionalText(requestBody, "bio", 500), pictureUrl(requestBody.get("pictureUrl")), optionalText(requestBody, "major", 100), graduationYear(requestBody.get("gradYear")), timestamp, timestamp);
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "duplicate_user", "Username or email already exists");
        }
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username=?", Long.class, username);
        if (userId == null) throw new IllegalStateException("Created user could not be loaded");
        replaceProfileArrays(userId, requestBody);
        invalidateUserReads(userId);
        return userId;
    }

    public long login(String username, String password) {
        Credentials credentials;
        try {
            credentials = jdbcTemplate.queryForObject("SELECT id,password_hash FROM users WHERE username=?", (resultSet, rowNumber) -> new Credentials(resultSet.getLong("id"), resultSet.getString("password_hash")), username.trim().toLowerCase(Locale.ROOT));
        } catch (EmptyResultDataAccessException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials", "Invalid username or password");
        }
        if (credentials == null || !passwordEncoder.matches(password, credentials.passwordHash()))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials", "Invalid username or password");
        return credentials.userId();
    }

    @Transactional
    public void update(long userId, Map<String, Object> requestBody) {
        String timestamp = Instant.now().toString();
        if (requestBody.containsKey("bio"))
            jdbcTemplate.update("UPDATE users SET bio=?,updated_at=? WHERE id=?", optionalText(requestBody, "bio", 500), timestamp, userId);
        if (requestBody.containsKey("pictureUrl"))
            jdbcTemplate.update("UPDATE users SET picture_url=?,updated_at=? WHERE id=?", pictureUrl(requestBody.get("pictureUrl")), timestamp, userId);
        if (requestBody.containsKey("gradYear"))
            jdbcTemplate.update("UPDATE users SET grad_year=?,updated_at=? WHERE id=?", graduationYear(requestBody.get("gradYear")), timestamp, userId);
        if (requestBody.containsKey("major"))
            jdbcTemplate.update("UPDATE users SET major=?,updated_at=? WHERE id=?", optionalText(requestBody, "major", 100), timestamp, userId);
        if (requestBody.keySet().stream().anyMatch(PROFILE_ARRAY_FIELDS::contains)) {
            Map<String, Object> completeProfile = new HashMap<>(profile(userId));
            completeProfile.putAll(requestBody);
            replaceProfileArrays(userId, completeProfile);
        }
        invalidateUserReads(userId);
    }

    private void validateRegistration(String username, String email) {
        if (!username.matches("[a-z0-9_-]+") || RESERVED_USERNAMES.contains(username))
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_username", "Username contains unsupported characters");
        if (!email.matches("^[^\\s@]+@calpoly\\.edu$"))
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_email", "A valid @calpoly.edu email is required");
    }

    private void replaceProfileArrays(long userId, Map<String, Object> profileData) {
        List<String> classes = stringArray(profileData.get("classes"), "classes", 30, 80);
        List<String> studying = stringArray(profileData.get("studying"), "studying", 30, 80);
        List<String> locations = stringArray(profileData.get("preferredStudyLocations"), "preferredStudyLocations", 20, 100);
        List<Integer> availableStudyTimes = studyTimes(profileData.get("studyTimes"));
        if (!classes.containsAll(studying))
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_studying", "studying must be a subset of classes");
        replaceTextArray(userId, "user_classes", "class_name", classes);
        replaceTextArray(userId, "user_studying", "class_name", studying);
        replaceTextArray(userId, "user_preferred_locations", "location", locations);
        jdbcTemplate.update("DELETE FROM user_study_times WHERE user_id=?", userId);
        for (Integer hourOfWeek : availableStudyTimes)
            jdbcTemplate.update("INSERT INTO user_study_times(user_id,hour_of_week) VALUES(?,?)", userId, hourOfWeek);
    }

    private void replaceTextArray(long userId, String tableName, String columnName, List<String> values) {
        jdbcTemplate.update("DELETE FROM " + tableName + " WHERE user_id=?", userId);
        for (String value : values)
            jdbcTemplate.update("INSERT INTO " + tableName + "(user_id," + columnName + ") VALUES(?,?)", userId, value);
    }

    public Users find(long userId) {
        return readCache.getOrLoad(profileCacheKey(userId), PROFILE_CACHE_TTL, () -> loadUser(userId));
    }

    private Users loadUser(long userId) {
        try {
            Users user = jdbcTemplate.queryForObject("SELECT id,username,email,bio,picture_url,major,grad_year,created_at,updated_at FROM users WHERE id=?", (resultSet, rowNumber) -> new Users(resultSet, textValues(userId, "user_classes", "class_name"), textValues(userId, "user_studying", "class_name"), jdbcTemplate.queryForList("SELECT hour_of_week FROM user_study_times WHERE user_id=? ORDER BY hour_of_week", Integer.class, userId), textValues(userId, "user_preferred_locations", "location")), userId);
            if (user == null) throw new EmptyResultDataAccessException(1);
            return user;
        } catch (EmptyResultDataAccessException exception) {
            throw new ApiException(HttpStatus.NOT_FOUND, "user_not_found", "User not found");
        }
    }

    public Map<String, Object> profile(long userId) {
        return find(userId).serialize(true);
    }

    private List<String> textValues(long userId, String tableName, String columnName) {
        return jdbcTemplate.queryForList("SELECT " + columnName + " FROM " + tableName + " WHERE user_id=? ORDER BY " + columnName, String.class, userId);
    }

    public boolean exists(long userId) {
        Boolean userExists = jdbcTemplate.query("SELECT EXISTS(SELECT 1 FROM users WHERE id=?)", resultSet -> {
            resultSet.next();
            return resultSet.getBoolean(1);
        }, userId);
        return Boolean.TRUE.equals(userExists);
    }

    private ReadCache.Key<Users> profileCacheKey(long userId) {
        return ReadCache.Key.of(PROFILE_CACHE_PREFIX + userId);
    }

    private void invalidateUserReads(long userId) {
        readCache.invalidate(profileCacheKey(userId));
        readCache.invalidatePrefix("recommendations:");
        readCache.invalidatePrefix("looking-now:");
    }

    private record Credentials(long userId, String passwordHash) {
    }
}
