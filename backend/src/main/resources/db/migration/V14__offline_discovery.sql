ALTER TABLE users ADD COLUMN offline_discoverable INTEGER NOT NULL DEFAULT 0
    CHECK (offline_discoverable IN (0, 1));
