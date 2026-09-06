PRAGMA foreign_keys = OFF;

CREATE TABLE match_decisions_replacement (
    actor_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    decision TEXT NOT NULL CHECK (decision IN ('accepted', 'rejected', 'deferred')),
    created_at TEXT NOT NULL,
    PRIMARY KEY (actor_user_id, target_user_id),
    CHECK (actor_user_id <> target_user_id)
);

INSERT INTO match_decisions_replacement (actor_user_id, target_user_id, decision, created_at)
SELECT actor_user_id, target_user_id, decision, created_at
FROM match_decisions;

DROP TABLE match_decisions;
ALTER TABLE match_decisions_replacement RENAME TO match_decisions;

CREATE INDEX IF NOT EXISTS idx_match_decisions_actor_created
    ON match_decisions(actor_user_id, created_at);

PRAGMA foreign_keys = ON;
