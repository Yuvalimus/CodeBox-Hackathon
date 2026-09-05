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

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
