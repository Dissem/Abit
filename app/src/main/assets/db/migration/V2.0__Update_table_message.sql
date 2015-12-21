-- This is done in V1.2, as SQLite doesn't support ADD CONSTRAINT and a proper migration
-- wasn't really necessary yet.
--
-- This file is here to reduce confusion regarding to the original migration files.

--ALTER TABLE Message ADD COLUMN initial_hash BINARY(64);
--ALTER TABLE Message ADD CONSTRAINT initial_hash_unique UNIQUE(initial_hash);