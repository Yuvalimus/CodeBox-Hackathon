PRAGMA foreign_keys = OFF;

CREATE TABLE users_replacement (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL COLLATE NOCASE,
    email TEXT NOT NULL COLLATE NOCASE UNIQUE,
    password_hash TEXT NOT NULL,
    bio TEXT NOT NULL DEFAULT '',
    picture_url TEXT,
    grad_year INTEGER,
    major TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

INSERT INTO users_replacement (id, username, email, password_hash, bio, picture_url, grad_year, major, created_at, updated_at)
SELECT id, username, email, password_hash, bio, picture_url, grad_year, major, created_at, updated_at
FROM users;

DROP TABLE users;
ALTER TABLE users_replacement RENAME TO users;

PRAGMA foreign_keys = ON;
