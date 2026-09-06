CREATE INDEX IF NOT EXISTS idx_match_decisions_actor_created
    ON match_decisions(actor_user_id, created_at);
