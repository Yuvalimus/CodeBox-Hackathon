CREATE TABLE IF NOT EXISTS users (
                                     id INTEGER PRIMARY KEY AUTOINCREMENT,
                                     username TEXT NOT NULL COLLATE NOCASE,
                                     email TEXT NOT NULL COLLATE NOCASE UNIQUE,
                                     password_hash TEXT NOT NULL,
                                     bio TEXT NOT NULL DEFAULT '',
                                     comments TEXT NOT NULL DEFAULT '',
                                     picture_url TEXT,
                                     grad_year INTEGER,
                                     major TEXT,
                                     created_at TEXT NOT NULL,
                                     updated_at TEXT NOT NULL
);


CREATE TABLE IF NOT EXISTS user_classes (
                                            user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    class_name TEXT NOT NULL,
    PRIMARY KEY (user_id, class_name)
    );


CREATE TABLE IF NOT EXISTS user_studying (
                                             user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    class_name TEXT NOT NULL,
    PRIMARY KEY (user_id, class_name)
    );


CREATE TABLE IF NOT EXISTS user_study_times (
                                                user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    hour_of_week INTEGER NOT NULL CHECK (hour_of_week BETWEEN 0 AND 671),
    PRIMARY KEY (user_id, hour_of_week)
    );


CREATE TABLE IF NOT EXISTS user_preferred_locations (
                                                        user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    location TEXT NOT NULL,
    PRIMARY KEY (user_id, location)
    );


CREATE TABLE IF NOT EXISTS match_decisions (
                                               actor_user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    target_user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    decision TEXT NOT NULL CHECK (decision IN ('accepted', 'rejected')),
    created_at TEXT NOT NULL,
    PRIMARY KEY (actor_user_id, target_user_id),
    CHECK (actor_user_id <> target_user_id)
    );


CREATE TABLE IF NOT EXISTS matches (
                                       id INTEGER PRIMARY KEY AUTOINCREMENT,
                                       user_a_id INTEGER NOT NULL REFERENCES users (id),
    user_b_id INTEGER NOT NULL REFERENCES users (id),
    created_at TEXT NOT NULL,
    UNIQUE (user_a_id, user_b_id),
    CHECK (user_a_id < user_b_id)
    );


CREATE TABLE IF NOT EXISTS chats (
                                     id TEXT PRIMARY KEY NOT NULL,
                                     created_at TEXT NOT NULL
);


CREATE TABLE IF NOT EXISTS chat_members (
                                            chat_id TEXT NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users (id),
    joined_at TEXT NOT NULL,
    PRIMARY KEY (chat_id, user_id)
    );


CREATE TABLE IF NOT EXISTS messages (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        chat_id TEXT NOT NULL REFERENCES chats (id) ON DELETE CASCADE,
    sender_user_id INTEGER NOT NULL REFERENCES users (id),
    body TEXT NOT NULL,
    created_at TEXT NOT NULL
    );


CREATE INDEX IF NOT EXISTS idx_messages_chat_created ON messages (chat_id, created_at, id);


CREATE TABLE IF NOT EXISTS looking_now (
                                           user_id INTEGER PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    subjects_json TEXT NOT NULL,
    expires_at TEXT NOT NULL,
    created_at TEXT NOT NULL
    );


CREATE INDEX IF NOT EXISTS idx_looking_now_expires ON looking_now (expires_at);
