PRAGMA foreign_keys = OFF;

CREATE TABLE chat_id_mapping (old_id INTEGER PRIMARY KEY, new_id TEXT NOT NULL UNIQUE);
INSERT INTO chat_id_mapping(old_id,new_id) SELECT id, lower(hex(randomblob(4))) || '-' || lower(hex(randomblob(2))) || '-4' || substr(lower(hex(randomblob(2))),2) || '-' || substr('89ab', abs(random()) % 4 + 1, 1) || substr(lower(hex(randomblob(2))),2) || '-' || lower(hex(randomblob(6))) FROM chats;

CREATE TABLE chats_replacement (id TEXT PRIMARY KEY NOT NULL, created_at TEXT NOT NULL);
INSERT INTO chats_replacement(id,created_at) SELECT mapping.new_id,chats.created_at FROM chats JOIN chat_id_mapping mapping ON mapping.old_id=chats.id;

CREATE TABLE chat_members_replacement (chat_id TEXT NOT NULL REFERENCES chats_replacement(id) ON DELETE CASCADE, user_id INTEGER NOT NULL REFERENCES users(id), joined_at TEXT NOT NULL, PRIMARY KEY(chat_id,user_id));
INSERT INTO chat_members_replacement(chat_id,user_id,joined_at) SELECT mapping.new_id,members.user_id,members.joined_at FROM chat_members members JOIN chat_id_mapping mapping ON mapping.old_id=members.chat_id;

CREATE TABLE messages_replacement (id INTEGER PRIMARY KEY AUTOINCREMENT, chat_id TEXT NOT NULL REFERENCES chats_replacement(id) ON DELETE CASCADE, sender_user_id INTEGER NOT NULL REFERENCES users(id), body TEXT NOT NULL, created_at TEXT NOT NULL);
INSERT INTO messages_replacement(id,chat_id,sender_user_id,body,created_at) SELECT messages.id,mapping.new_id,messages.sender_user_id,messages.body,messages.created_at FROM messages JOIN chat_id_mapping mapping ON mapping.old_id=messages.chat_id;

DROP TABLE messages;
DROP TABLE chat_members;
DROP TABLE chats;
ALTER TABLE chats_replacement RENAME TO chats;
ALTER TABLE chat_members_replacement RENAME TO chat_members;
ALTER TABLE messages_replacement RENAME TO messages;
CREATE INDEX idx_messages_chat_created ON messages (chat_id, created_at, id);
DROP TABLE chat_id_mapping;

PRAGMA foreign_keys = ON;
