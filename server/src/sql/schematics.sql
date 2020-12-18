-- drop database tables for testing (topologically sorted)
-- DROP TABLE IF EXISTS package_bins;
-- DROP TABLE IF EXISTS package_entries;
-- DROP TABLE IF EXISTS package_blobs;
-- DROP TABLE IF EXISTS package_labels;
-- DROP TABLE IF EXISTS user_request_history;
-- DROP TABLE IF EXISTS user_authorizations;
-- DROP FUNCTION IF EXISTS version(TEXT);
-- DROP FUNCTION IF EXISTS build_date(TEXT);



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
-- e.g. build_date('2020.09.03.14.57/g8TiZeAvyMMhQvEOfkiCTA') = '2020.09.03.14.57'
CREATE OR REPLACE FUNCTION build_date(buildlabel TEXT) RETURNS TEXT
AS
    'SELECT split_part(buildlabel, ''/'', 1)'
    LANGUAGE 'sql'
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;


-- Stores Packages.bin version metadata
CREATE TABLE IF NOT EXISTS package_labels
(
    -- approximate fetch time
    timestamp             TIMESTAMP NOT NULL /* INDEXED */,
    -- partial or full build label from w-state (e.g. '2020.09.03.14.57', '2020.09.03.14.57/g8TiZeAvyMMhQvEOfkiCTA')
    buildlabel            TEXT      NOT NULL /* INDEXED */,
    -- optional semver-esque format declared in forum update post (e.g. '29.0.1.3') (but not semver compliant)
    semver                TEXT      CHECK (semver ~ '^\d+(?:\.\d+)*$'),
    -- optional forum post URL where the `semver` was obtained
    forum_url             TEXT,
    -- optional linked steam depot manifest id (if this entry is from a steam database)
    steam_manifest_id     BIGINT,

    -- partial build version (e.g. '2020.09.03.14.57')
    build_date            TEXT      PRIMARY KEY NOT NULL GENERATED ALWAYS AS (build_date(buildlabel)) STORED /* INDEXED */,
    -- build_date ordinal for ranged/order operations
    build_date_ordinal    BIGINT    GENERATED ALWAYS AS (version(build_date(buildlabel))) STORED /* INDEXED */,
    -- semver ordinal for ranged/order operations
    forum_version_ordinal BIGINT    GENERATED ALWAYS AS (version(semver)) STORED /* INDEXED */
);
CREATE INDEX IF NOT EXISTS packages__timestamp ON package_labels (timestamp);
CREATE INDEX IF NOT EXISTS packages__forum_version ON package_labels (semver);
CREATE INDEX IF NOT EXISTS packages__build_date_ordinal ON package_labels (build_date_ordinal);
CREATE INDEX IF NOT EXISTS packages__forum_version_ordinal ON package_labels (forum_version_ordinal);


-- Stores compressed Packages.bin
CREATE TABLE IF NOT EXISTS package_bins (
    build_date TEXT  PRIMARY KEY NOT NULL REFERENCES package_labels (build_date),
    -- compressed Packages.bin file
    packages   BYTEA NOT NULL
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
    build_date TEXT  NOT NULL REFERENCES package_labels (build_date) /* INDEXED */,
    -- package's absolute path (e.g. '/Package/Path/X')
    path       TEXT  NOT NULL /* INDEXED */,
    -- package's content hash
    sha256     BYTEA NOT NULL REFERENCES package_blobs (sha256),

    PRIMARY KEY (build_date, path)
);
CREATE INDEX IF NOT EXISTS package_entries__build_date ON package_entries (build_date);
CREATE INDEX IF NOT EXISTS package_entries__path ON package_entries (path);


-- noinspection SqlResolve @ routine/"gen_random_uuid"
CREATE TABLE IF NOT EXISTS user_authorizations
(
    -- 32 hex token; mutable
    token         TEXT    PRIMARY KEY NOT NULL CHECK (token ~ '^[0-9A-F]{32}$'),
    -- unique user identifier (used as a foreign key)
    uuid          UUID    UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    -- logging display name (non-zero length); mutable
    display_name  TEXT    NOT NULL CHECK (display_name ~ '^.+$'),
    -- indicates the user has administrator privileges (e.g. can create new users)
    administrator BOOLEAN DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS user_authorizations__uuid ON user_authorizations (uuid);

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
CREATE INDEX IF NOT EXISTS user_request_history__user_uuid ON user_request_history (user_uuid);
CREATE INDEX IF NOT EXISTS user_request_history__ip ON user_request_history (ip);
