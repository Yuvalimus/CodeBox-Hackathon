PRAGMA foreign_keys = OFF;

CREATE TABLE user_study_times_replacement (
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    hour_of_week INTEGER NOT NULL CHECK (hour_of_week BETWEEN 0 AND 671),
    PRIMARY KEY (user_id, hour_of_week)
);

INSERT INTO user_study_times_replacement(user_id,hour_of_week)
SELECT user_id,hour_of_week * 4 FROM user_study_times;

DROP TABLE user_study_times;
ALTER TABLE user_study_times_replacement RENAME TO user_study_times;

PRAGMA foreign_keys = ON;
