CREATE TABLE IF NOT EXISTS chat_seen_state (
    chat_id TEXT NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    seen_at TEXT,
    PRIMARY KEY (chat_id, user_id)
);

INSERT OR IGNORE INTO chat_seen_state(chat_id, user_id, seen_at)
SELECT cm.chat_id, cm.user_id, c.created_at
FROM chat_members cm JOIN chats c ON c.id=cm.chat_id;
