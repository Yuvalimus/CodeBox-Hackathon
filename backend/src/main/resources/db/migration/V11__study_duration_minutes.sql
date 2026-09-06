ALTER TABLE users ADD COLUMN study_duration_minutes INTEGER NOT NULL DEFAULT 60
    CHECK (study_duration_minutes BETWEEN 15 AND 480 AND study_duration_minutes % 15 = 0);

DROP TABLE user_study_times;
