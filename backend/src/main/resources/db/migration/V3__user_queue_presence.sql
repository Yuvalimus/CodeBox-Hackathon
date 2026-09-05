CREATE TABLE IF NOT EXISTS user_queue_presence (
    user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    last_heartbeat_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_queue_presence_heartbeat ON user_queue_presence(last_heartbeat_at);
