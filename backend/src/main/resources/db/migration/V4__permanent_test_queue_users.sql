CREATE TABLE IF NOT EXISTS permanent_test_queue_users (
    user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE
);

INSERT OR IGNORE INTO permanent_test_queue_users(user_id)
SELECT id FROM users WHERE email LIKE 'test-%@calpoly.edu';
