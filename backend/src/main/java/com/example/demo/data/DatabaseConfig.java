package com.example.demo.data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.sqlite.SQLiteConfig;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
public class DatabaseConfig {
    @Bean
    public DataSource dataSource(@Value("${app.database-url}") String databaseUrl) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(databaseUrl);
        SQLiteConfig sqlite = new SQLiteConfig();
        sqlite.enforceForeignKeys(true);
        dataSource.setConnectionProperties(sqlite.toProperties());
        try (var connection = dataSource.getConnection()) {
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V1__initial_schema.sql"));
            if (usernameHasUniqueConstraint(connection)) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V2__allow_duplicate_usernames.sql"));
            }
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V3__user_queue_presence.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V4__permanent_test_queue_users.sql"));
            if (!usersHasColumn(connection, "comments")) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V5__user_comments.sql"));
            }
            if (!usersHasColumn(connection, "avatar")) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V6__user_avatar.sql"));
            }
            if (!chatIdsAreUuid(connection)) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V7__chat_uuid_ids.sql"));
            }
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V8__site_presence.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V9__match_decision_expiry.sql"));
            if (!studyTimesSupportQuarterHours(connection)) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V10__quarter_hour_study_times.sql"));
            }
            if (!usersHasColumn(connection, "study_duration_minutes")) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V11__study_duration_minutes.sql"));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize SQLite schema", e);
        }
        return dataSource;
    }

    private boolean usernameHasUniqueConstraint(java.sql.Connection connection) throws java.sql.SQLException {
        try (var statement = connection.prepareStatement("SELECT sql FROM sqlite_master WHERE type='table' AND name='users'")) {
            try (var result = statement.executeQuery()) {
                return result.next() && result.getString(1).matches("(?is).*username\\s+TEXT\\s+NOT\\s+NULL\\s+COLLATE\\s+NOCASE\\s+UNIQUE.*");
            }
        }
    }

    private boolean usersHasColumn(java.sql.Connection connection, String columnName) throws java.sql.SQLException {
        try (var statement = connection.prepareStatement("PRAGMA table_info(users)")) {
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    if (columnName.equalsIgnoreCase(result.getString("name"))) return true;
                }
                return false;
            }
        }
    }

    private boolean chatIdsAreUuid(java.sql.Connection connection) throws java.sql.SQLException {
        try (var statement = connection.prepareStatement("PRAGMA table_info(chats)")) {
            try (var result = statement.executeQuery()) {
                return result.next() && "TEXT".equalsIgnoreCase(result.getString("type"));
            }
        }
    }

    private boolean studyTimesSupportQuarterHours(java.sql.Connection connection) throws java.sql.SQLException {
        try (var statement = connection.prepareStatement("SELECT sql FROM sqlite_master WHERE type='table' AND name='user_study_times'")) {
            try (var result = statement.executeQuery()) {
                // V11 removes the table altogether; no older time-slot migration is needed then.
                return !result.next() || result.getString(1).matches("(?is).*hour_of_week.*BETWEEN\\s+0\\s+AND\\s+671.*");
            }
        }
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
