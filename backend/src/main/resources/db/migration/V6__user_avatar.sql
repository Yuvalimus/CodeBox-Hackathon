ALTER TABLE users ADD COLUMN avatar TEXT NOT NULL DEFAULT 'sage'
    CHECK (avatar IN ('sage', 'blue', 'peach', 'lavender'));
