CREATE TABLE IF NOT EXISTS user_site_presence (
    user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    last_heartbeat_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_site_presence_heartbeat ON user_site_presence(last_heartbeat_at);
