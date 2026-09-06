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
    private static final Set<String> PROFILE_ARRAY_FIELDS = Set.of("classes", "studying", "studyTimes", "preferredStudyLocations");
    private static final Set<String> PROFILE_FIELDS = Set.of("username", "email", "bio", "comments", "pictureUrl", "avatar", "gradYear", "major", "classes", "studying", "studyTimes", "preferredStudyLocations");
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

    public static String requiredText(Object rawValue, String fieldName, int minimumLength, int maximumLength) {
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

    private static String optionalText(Object rawValue, String fieldName, int maximumLength) {
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

    private static String avatar(Object rawValue) {
        if (rawValue == null) return "sage";
        if (!(rawValue instanceof String))
            throwInvalid("avatar", "avatar is invalid");
        String value = ((String) rawValue).trim();
        if (!Set.of("sage", "blue", "peach", "lavender").contains(value))
            throwInvalid("avatar", "avatar is invalid");
        return value;
    }

    private static void throwInvalid(String fieldName, String message) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_" + fieldName, message);
    }

    @Transactional
    public long register(Registration registration) {
        String username = username(registration.username());
        String email = email(registration.email());
        String password = requiredText(registration.password(), "password", 8, 200);
        ensureEmailAvailable(email, null);
        String timestamp = Instant.now().toString();
        try {
            jdbcTemplate.update("INSERT INTO users(username,email,password_hash,bio,comments,picture_url,avatar,major,grad_year,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)", username, email, passwordEncoder.encode(password), optionalText(registration.bio(), "bio", 500), optionalText(registration.comments(), "comments", 500), pictureUrl(registration.pictureUrl()), avatar(registration.avatar()), optionalText(registration.major(), "major", 100), graduationYear(registration.gradYear()), timestamp, timestamp);
        } catch (DataIntegrityViolationException exception) {
            throw emailAlreadyUsed();
        }
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email=?", Long.class, email);
        if (userId == null) throw new IllegalStateException("Created user could not be loaded");
        replaceProfileArrays(userId, registration.classes(), registration.studying(), registration.preferredStudyLocations(), registration.studyTimes());
        invalidateUserReads(userId);
        return userId;
    }

    public long login(String email, String password) {
        Credentials credentials;
        try {
            credentials = jdbcTemplate.queryForObject("SELECT id,password_hash FROM users WHERE email=?", (resultSet, rowNumber) -> new Credentials(resultSet.getLong("id"), resultSet.getString("password_hash")), email(email));
        } catch (EmptyResultDataAccessException exception) {
            throw invalidCredentials();
        }
        if (credentials == null || !passwordEncoder.matches(password, credentials.passwordHash()))
            throw invalidCredentials();
        return credentials.userId();
    }

    @Transactional
    public void update(long userId, Map<String, Object> requestBody) {
        if (requestBody.isEmpty() || requestBody.keySet().stream().anyMatch(field -> !PROFILE_FIELDS.contains(field)))
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_profile", "Request contains no editable profile fields");
        String timestamp = Instant.now().toString();
        if (requestBody.containsKey("username"))
            jdbcTemplate.update("UPDATE users SET username=?,updated_at=? WHERE id=?", username(requestBody.get("username")), timestamp, userId);
        if (requestBody.containsKey("email")) {
            String email = email(requestBody.get("email"));
            ensureEmailAvailable(email, userId);
            try {
                jdbcTemplate.update("UPDATE users SET email=?,updated_at=? WHERE id=?", email, timestamp, userId);
            } catch (DataIntegrityViolationException exception) {
                throw emailAlreadyUsed();
            }
        }
        if (requestBody.containsKey("bio"))
            jdbcTemplate.update("UPDATE users SET bio=?,updated_at=? WHERE id=?", optionalText(requestBody.get("bio"), "bio", 500), timestamp, userId);
        if (requestBody.containsKey("comments"))
            jdbcTemplate.update("UPDATE users SET comments=?,updated_at=? WHERE id=?", optionalText(requestBody.get("comments"), "comments", 500), timestamp, userId);
        if (requestBody.containsKey("pictureUrl"))
            jdbcTemplate.update("UPDATE users SET picture_url=?,updated_at=? WHERE id=?", pictureUrl(requestBody.get("pictureUrl")), timestamp, userId);
        if (requestBody.containsKey("avatar"))
            jdbcTemplate.update("UPDATE users SET avatar=?,updated_at=? WHERE id=?", avatar(requestBody.get("avatar")), timestamp, userId);
        if (requestBody.containsKey("gradYear"))
            jdbcTemplate.update("UPDATE users SET grad_year=?,updated_at=? WHERE id=?", graduationYear(requestBody.get("gradYear")), timestamp, userId);
        if (requestBody.containsKey("major"))
            jdbcTemplate.update("UPDATE users SET major=?,updated_at=? WHERE id=?", optionalText(requestBody.get("major"), "major", 100), timestamp, userId);
        if (requestBody.keySet().stream().anyMatch(PROFILE_ARRAY_FIELDS::contains)) {
            Map<String, Object> completeProfile = new HashMap<>(find(userId).serialize(true));
            completeProfile.putAll(requestBody);
            replaceProfileArrays(userId, completeProfile.get("classes"), completeProfile.get("studying"),
                completeProfile.get("preferredStudyLocations"), completeProfile.get("studyTimes"));
        }
        invalidateUserReads(userId);
    }

    private static String username(Object rawValue) {
        if (!(rawValue instanceof String)) throwInvalid("username", "username is invalid");
        String value = ((String) rawValue).trim();
        if (value.isEmpty() || value.length() > 32 || value.codePoints().anyMatch(Character::isISOControl))
            throwInvalid("username", "username is invalid");
        return value;
    }

    private static String email(Object rawValue) {
        if (!(rawValue instanceof String)) throwInvalid("email", "email is invalid");
        String value = ((String) rawValue).trim().toLowerCase(Locale.ROOT);
        if (value.length() < 6 || value.length() > 254) throwInvalid("email", "email is invalid");
        if (!value.matches("^[^\\s@]+@calpoly\\.edu$"))
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_email", "A valid @calpoly.edu email is required");
        return value;
    }

    private void ensureEmailAvailable(String email, Long exceptUserId) {
        String query = exceptUserId == null
            ? "SELECT COUNT(*) FROM users WHERE email=?"
            : "SELECT COUNT(*) FROM users WHERE email=? AND id<>?";
        Integer count = exceptUserId == null
            ? jdbcTemplate.queryForObject(query, Integer.class, email)
            : jdbcTemplate.queryForObject(query, Integer.class, email, exceptUserId);
        if (count != null && count > 0) throw emailAlreadyUsed();
    }

    private ApiException emailAlreadyUsed() {
        return new ApiException(HttpStatus.CONFLICT, "email_already_used", "This email has already been used");
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials", "Invalid username or password");
    }

    private void replaceProfileArrays(long userId, Object classesValue, Object studyingValue, Object locationsValue, Object studyTimesValue) {
        List<String> classes = stringArray(classesValue, "classes", 30, 80);
        List<String> studying = stringArray(studyingValue, "studying", 30, 80);
        List<String> locations = stringArray(locationsValue, "preferredStudyLocations", 20, 100);
        List<Integer> availableStudyTimes = studyTimes(studyTimesValue);
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
            Users user = jdbcTemplate.queryForObject("SELECT id,username,email,bio,comments,picture_url,avatar,major,grad_year,created_at,updated_at FROM users WHERE id=?", (resultSet, rowNumber) -> new Users(resultSet, textValues(userId, "user_classes", "class_name"), textValues(userId, "user_studying", "class_name"), jdbcTemplate.queryForList("SELECT hour_of_week FROM user_study_times WHERE user_id=? ORDER BY hour_of_week", Integer.class, userId), textValues(userId, "user_preferred_locations", "location")), userId);
            if (user == null) throw new EmptyResultDataAccessException(1);
            return user;
        } catch (EmptyResultDataAccessException exception) {
            throw new ApiException(HttpStatus.NOT_FOUND, "user_not_found", "User not found");
        }
    }

    public Users.Profile profile(long userId) {
        return find(userId).profile(true);
    }

    public Users.PublicProfile publicProfile(long userId) {
        return find(userId).publicProfile();
    }

    @Transactional
    public void updatePictureUrl(long userId, String pictureUrl) {
        jdbcTemplate.update("UPDATE users SET picture_url=?,updated_at=? WHERE id=?", pictureUrl, Instant.now().toString(), userId);
        invalidateUserReads(userId);
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

    /** Fully typed registration command, validated by this service before persistence. */
    public record Registration(
        String username,
        String password,
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
        List<String> preferredStudyLocations) { }
}
