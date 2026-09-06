ALTER TABLE user_queue_presence ADD COLUMN last_swipe_at TEXT;
UPDATE user_queue_presence SET last_swipe_at=last_heartbeat_at WHERE last_swipe_at IS NULL;
