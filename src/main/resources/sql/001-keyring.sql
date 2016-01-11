CREATE SCHEMA sso;

CREATE TABLE sso.keyring (
	id BIGSERIAL PRIMARY KEY,
	name VARCHAR(63) not null unique,
	service_id VARCHAR(63) not null unique,
	url VARCHAR(1024),
	description TEXT,
	form_schema JSONB not null
);

CREATE TABLE sso.scripts (
	filename VARCHAR(255) NOT NULL PRIMARY KEY,
	passed TIMESTAMP NOT NULL DEFAULT NOW()
);
