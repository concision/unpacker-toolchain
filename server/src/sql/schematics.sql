-- drop database tables for testing (topologically sorted)
-- DROP TABLE IF EXISTS package_entries;
-- DROP TABLE IF EXISTS package_blobs;
-- DROP TABLE IF EXISTS packages;


-- Stores Packages.bin and version metadata
CREATE TABLE IF NOT EXISTS packages
(
    -- approximate fetch time
    timestamp             TIMESTAMP NOT NULL /* INDEXED */,
    -- partial or full build label from w-state (e.g. '2020.09.03.14.57', '2020.09.03.14.57/g8TiZeAvyMMhQvEOfkiCTA')
    build_label           TEXT      NOT NULL /* INDEXED */,
    -- optional semver-esque format declared in forum update post (e.g. '29.0.1.3') (but not semver compliant)
    forum_version         TEXT      CHECK (forum_version ~ '^\d+(?:\.\d+)*$'),
    -- optional forum post URL where the `forum_version` was obtained
    forum_url             TEXT,
    -- compressed Packages.bin file
    packages              BYTEA     NOT NULL,

    -- partial build version (e.g. '2020.09.03.14.57')
    build_version         TEXT      PRIMARY KEY GENERATED ALWAYS AS (split_part(build_label, '/', 1)) STORED /* INDEXED */,

    -- build_version ordinal for ranged/order operations
    build_version_ordinal BIGINT    GENERATED ALWAYS AS (version(split_part(build_label, '/', 1))) STORED /* INDEXED */,
    -- forum_version ordinal for ranged/order operations
    forum_version_ordinal BIGINT    GENERATED ALWAYS AS (version(forum_version)) STORED /* INDEXED */
);
CREATE INDEX packages__timestamp ON packages (timestamp);
CREATE INDEX packages__forum_version ON packages (forum_version);
CREATE INDEX packages__build_version_ordinal ON packages (build_version_ordinal);
CREATE INDEX packages__forum_version_ordinal ON packages (forum_version_ordinal);


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
    build_version TEXT  NOT NULL REFERENCES packages (build_version) /* INDEXED */,
    -- package's absolute path (e.g. '/Package/Path/X')
    path          TEXT  NOT NULL /* INDEXED */,
    -- package's content hash
    sha256        BYTEA NOT NULL REFERENCES package_blobs (sha256),

    PRIMARY KEY (build_version, path)
);
CREATE INDEX package_entries__build_version ON package_entries (build_version);
CREATE INDEX package_entries__path ON package_entries (path);



-- converts forum or build versions (e.g. '29', '29.0.3', '2020.09.03.14.57', etc) to BIGINT (8-byte) ordinals
-- supports up to 5 fragments; each fragment is allocated 12 bits, however the first is allocated 16 bits
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
