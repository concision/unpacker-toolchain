-- drop database tables for testing (topologically sorted)
-- DROP TABLE IF EXISTS package_bins;
-- DROP TABLE IF EXISTS package_entries;
-- DROP TABLE IF EXISTS package_blobs;
-- DROP TABLE IF EXISTS package_labels;
-- DROP TABLE IF EXISTS user_authorizations;
-- DROP TABLE IF EXISTS user_request_history;


-- Enable UUID extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


-- Converts forum or build versions (e.g. '29', '29.0.3', '2020.09.03.14.57', etc) to BIGINT (8-byte) ordinals.
-- Up to 5 fragments are supported; each fragment is allocated 12 bits, however the first is allocated 16 bits.
-- binary: 0000 0000 0000 0000 | 0000 0000 0000 | 0000 0000 0000 | 0000 0000 0000 | 0000 0000 0000
CREATE OR REPLACE FUNCTION version(version TEXT) RETURNS BIGINT
AS
'
    SELECT bit_or(coalesce(nullif(split_part(version, ''.'', i), ''''), ''0'')::int::bit(64) << (12 * (5 - i)))::bigint
    FROM generate_series(1, 5) AS i
'
    LANGUAGE 'sql'
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;

-- Extracts build version from a build label.
-- e.g. build_version('2020.09.03.14.57/g8TiZeAvyMMhQvEOfkiCTA') = '2020.09.03.14.57'
CREATE OR REPLACE FUNCTION build_version(build_label TEXT) RETURNS TEXT
AS
    'SELECT split_part(build_label, ''/'', 1)'
    LANGUAGE 'sql'
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;


-- Stores Packages.bin version metadata
CREATE TABLE IF NOT EXISTS package_labels
(
    -- approximate fetch time
    timestamp             TIMESTAMP NOT NULL /* INDEXED */,
    -- partial or full build label from w-state (e.g. '2020.09.03.14.57', '2020.09.03.14.57/g8TiZeAvyMMhQvEOfkiCTA')
    build_label           TEXT      NOT NULL /* INDEXED */,
    -- optional semver-esque format declared in forum update post (e.g. '29.0.1.3') (but not semver compliant)
    forum_version         TEXT      CHECK (forum_version ~ '^\d+(?:\.\d+)*$'),
    -- optional forum post URL where the `forum_version` was obtained
    forum_url             TEXT,

    -- partial build version (e.g. '2020.09.03.14.57')
    build_version         TEXT      PRIMARY KEY NOT NULL GENERATED ALWAYS AS (build_version(build_label)) STORED /* INDEXED */,

    -- build_version ordinal for ranged/order operations
    build_version_ordinal BIGINT    GENERATED ALWAYS AS (version(build_version(build_label))) STORED /* INDEXED */,
    -- forum_version ordinal for ranged/order operations
    forum_version_ordinal BIGINT    GENERATED ALWAYS AS (version(forum_version)) STORED /* INDEXED */
);
CREATE INDEX packages__timestamp ON package_labels (timestamp);
CREATE INDEX packages__forum_version ON package_labels (forum_version);
CREATE INDEX packages__build_version_ordinal ON package_labels (build_version_ordinal);
CREATE INDEX packages__forum_version_ordinal ON package_labels (forum_version_ordinal);


-- Stores compressed Packages.bin
CREATE TABLE IF NOT EXISTS package_bins (
    build_version TEXT  PRIMARY KEY NOT NULL REFERENCES package_labels (build_version),
    -- compressed Packages.bin file
    packages      BYTEA NOT NULL
);


-- Package entries do not significantly change on each update and thus have a high duplication rate.
-- Rather than store duplicate content JSON blobs for each (game version, package), a content blob pool is used, indexed by a hash.
CREATE TABLE IF NOT EXISTS package_blobs
(
    -- a hash of the stringified package contents (e.g SHA256(contents::TEXT))
    -- this will likely not have collisions; otherwise we have found the world's first SHA256 collision.
    sha256   BYTEA PRIMARY KEY NOT NULL CHECK (length(sha256) = 32),
    -- JSONified package blob; JSON data is used to preserve key order
    contents JSON  NOT NULL
);


-- Stores package entries with an foreign key reference to the package content in the blob pool.
CREATE TABLE IF NOT EXISTS package_entries
(
    -- partial build version (e.g. '2020.09.03.14.57')
    build_version TEXT  NOT NULL REFERENCES package_labels (build_version) /* INDEXED */,
    -- package's absolute path (e.g. '/Package/Path/X')
    path          TEXT  NOT NULL /* INDEXED */,
    -- package's content hash
    sha256        BYTEA NOT NULL REFERENCES package_blobs (sha256),

    PRIMARY KEY (build_version, path)
);
CREATE INDEX package_entries__build_version ON package_entries (build_version);
CREATE INDEX package_entries__path ON package_entries (path);


-- noinspection SqlResolve @ routine/"uuid_generate_v4"
CREATE TABLE IF NOT EXISTS user_authorizations
(
    -- 32 hex token; mutable
    token        TEXT PRIMARY KEY NOT NULL CHECK (token ~ '^[0-9A-F]{32}$'),
    -- unique user identifier (used as a foreign key)
    uuid         UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(),
    -- logging display name (non-zero length); mutable
    display_name TEXT NOT NULL CHECK (display_name ~ '^.+$')
);
CREATE INDEX user_authorizations__uuid ON user_authorizations (uuid);

-- HTTP API request history of a user
CREATE TABLE IF NOT EXISTS user_request_history
(
    -- user who executed request
    user_uuid   UUID    NOT NULL REFERENCES user_authorizations (uuid),
    -- ip address of executing
    ip          INET    NOT NULL,
    -- request HTTP method (e.g. GET, POST, etc)
    http_method TEXT    NOT NULL,
    -- requested URL (potentially includes authorization if using auth query type)
    url         TEXT    NOT NULL CHECK (url ~ '^/.*$'),
    -- optional request body
    body        TEXT,
    -- request success
    success     BOOLEAN
);
CREATE INDEX user_request_history__user_uuid ON user_request_history (user_uuid);
CREATE INDEX user_request_history__ip ON user_request_history (ip);
