-- KNOWHUB PostgreSQL database setup
-- Run these statements while connected to the PostgreSQL maintenance database (usually postgres).
-- CREATE DATABASE cannot run inside a normal transaction. Execute each statement separately
-- in pgAdmin Query Tool with autocommit enabled, or run this file with psql in autocommit mode.
-- Existing databases are not modified or deleted.

CREATE DATABASE knowhub_auth;
CREATE DATABASE knowhub_course;
CREATE DATABASE knowhub_access;
CREATE DATABASE knowhub_assignment;
CREATE DATABASE knowhub_usage;
CREATE DATABASE knowhub_ai;

-- If a database already exists, skip that statement and continue with the others.
