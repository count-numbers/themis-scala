DROP TABLE "source";
DROP TABLE tagging;
DROP TABLE dtag;
DROP TABLE comment;
DROP TABLE link;
DROP TABLE attachment;
DROP TABLE activity;
DROP TABLE document;
DROP TABLE "user";
DROP TABLE contact;
DROP TABLE "config";

CREATE TABLE "user" (
   id SERIAL PRIMARY KEY,
   username VARCHAR NOT NULL UNIQUE,
   name VARCHAR NOT NULL,
   email VARCHAR NOT NULL,
   password VARCHAR NOT NULL
);

CREATE TABLE contact (
    id SERIAL PRIMARY KEY,
    identifier VARCHAR NOT NULL UNIQUE,
    name VARCHAR NOT NULL,
    address1 VARCHAR,
    address2 VARCHAR,
    zip VARCHAR,
    city VARCHAR,
    region VARCHAR,
    country VARCHAR,
    email VARCHAR,
    keywords VARCHAR
);

CREATE TABLE document (
   id SERIAL PRIMARY KEY,
   name VARCHAR NOT NULL,
   description TEXT,

   owner INT REFERENCES "user"(id) NOT NULL,
   contact INT REFERENCES contact(id),
   archiveTimestamp TIMESTAMP NOT NULL,
   modificationTimestamp TIMESTAMP NOT NULL,
   followUpTimestamp TIMESTAMP,

   sourceId VARCHAR NOT NULL,
   sourceReference VARCHAR NOT NULL,

   archivingComplete BOOLEAN NOT NULL,
   actionRequired BOOLEAN NOT NULL,

   fulltext TSVECTOR
);

CREATE TABLE dtag (
    id SERIAL PRIMARY KEY,
    name VARCHAR NOT NULL
);

CREATE TABLE tagging (
    docId INT REFERENCES document(id) NOT NULL,
    tagId INT REFERENCES dtag(id) NOT NULL
);

CREATE TABLE comment (
    id SERIAL PRIMARY KEY,
    docId INT REFERENCES document(id) NOT NULL,
    userId INT REFERENCES "user"(id) NOT NULL,
    text VARCHAR NOT NULL,
    timestamp TIMESTAMP NOT NULL
);

CREATE TABLE link (
    id SERIAL PRIMARY KEY,
    docId INT REFERENCES document(id) NOT NULL,
    title VARCHAR NOT NULL,
    url VARCHAR NOT NULL,
    linkType VARCHAR NOT NULL
);

CREATE TABLE attachment (
    id SERIAL PRIMARY KEY,
    docId INT NOT NULL,
    name VARCHAR NOT NULL,
    size BIGINT NOT NULL,
    mimeType VARCHAR NOT NULL
);

CREATE TABLE activity (
    id SERIAL PRIMARY KEY,
    docId INT REFERENCES document(id) NOT NULL,
    userId INT REFERENCES "user"(id) NOT NULL,
    arguments VARCHAR,
    activityType VARCHAR NOT NULL,
    timestamp TIMESTAMP NOT NULL
);


CREATE TABLE config (
    key  VARCHAR NOT NULL PRIMARY KEY,
    value VARCHAR NOT NULL
);

CREATE TABLE source (
    id SERIAL PRIMARY KEY,
    type VARCHAR NOT NULL,
    active BOOLEAN NOT NULL,
    "userId" INT REFERENCES "user"(id) NOT NULL,
    "gdriveSourceFolder" VARCHAR,
    "gdriveArchiveFolder" VARCHAR,
    "fileSourceFolder" VARCHAR
);

CREATE TABLE ingestion_log (
    id SERIAL PRIMARY KEY,
    level VARCHAR NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    "docId" INT,
    "srcId" INT,
    "userName" VARCHAR,
    text VARCHAR NOT NULL
);

-- Fulltext setup. Create trigger to concatenate name and description into fulltext column and create index
CREATE TRIGGER tsvectorupdate BEFORE INSERT OR UPDATE
ON document FOR EACH ROW EXECUTE PROCEDURE
tsvector_update_trigger(fulltext, 'pg_catalog.english', name, description);

CREATE INDEX textsearch_idx ON document USING GIN (fulltext);

INSERT INTO "user" (username, name, email, password) VALUES
    ('admin', 'Administrator', 'example@example.com', 'password'); -- 1

-- This may be useful:
-- INSERT INTO config VALUES ('google.oauth.client_id', 'xxx');
-- INSERT INTO config VALUES ('google.oauth.client_secret', 'xxx');
