-- Offline discovery is the default. Run this data migration only once so later
-- user opt-outs remain respected across application restarts.
CREATE TABLE IF NOT EXISTS app_data_migrations (
    name TEXT PRIMARY KEY
);

UPDATE users SET offline_discoverable=1
WHERE offline_discoverable=0
  AND NOT EXISTS (SELECT 1 FROM app_data_migrations WHERE name='V16__enable_offline_discovery_by_default');

INSERT OR IGNORE INTO app_data_migrations(name) VALUES('V16__enable_offline_discovery_by_default');
