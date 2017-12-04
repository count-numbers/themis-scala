DROP TABLE tagging;
DROP TABLE dtag;
DROP TABLE comment;
DROP TABLE link;
DROP TABLE attachment;
DROP TABLE activity;
DROP TABLE document;
DROP TABLE "user";
DROP TABLE contact;

CREATE TABLE "user" (
   id SERIAL PRIMARY KEY,
   username VARCHAR NOT NULL,
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
    email VARCHAR
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


-- Fulltext setup. Create trigger to concatenate name and description into fulltext column and create index
CREATE TRIGGER tsvectorupdate BEFORE INSERT OR UPDATE
ON document FOR EACH ROW EXECUTE PROCEDURE
tsvector_update_trigger(fulltext, 'pg_catalog.english', name, description);

CREATE INDEX textsearch_idx ON document USING GIN (fulltext);




INSERT INTO "user" (username, name, email, password) VALUES
    ('admin', 'Administrator', 'example@example.com', 'password'); -- 1

INSERT INTO contact (identifier, name, address1, zip, city, region, country, email) VALUES
    ('sender', 'The Sender', '5th Avenue', '12345', 'Vosshoefen', 'NRW', 'Germany', 'sender@example.com'); -- 1

INSERT INTO document (name, description, owner, contact, archiveTimestamp, modificationTimestamp, sourceId, sourceReference, archivingComplete, actionRequired) VALUES
  ('Spotflix Prime Invoice', 'Invoice for your entertainment package', 1, NULL, '2017-01-15 08:00', '2017-01-16 14:30', 'demo', 'demo-1', FALSE, TRUE), -- 1
  ('Homesafe', 'Send the funds and sleep well.', 1, 1, '2017-02-07 09:00', '2017-02-21 19:00', 'demo', 'demo-2', FALSE, TRUE);                       -- 2

INSERT INTO dtag (name) VALUES
    ('invoice'),        -- 1
    ('information'),    -- 2
    ('insurance'),      -- 3
    ('private'),        -- 4
    ('home');           -- 5

INSERT INTO tagging (docId, tagId) VALUES
    (1, 1),
    (1, 4),
    (2, 1),
    (2, 3),
    (2, 5);

INSERT INTO comment (docId, userId, text, timestamp) VALUES
    (2, 1, 'This is a comment', '2017-02-14 10:00'),
    (2, 1, 'Here is another comment', '2017-02-17 15:00');

INSERT INTO link (docId, title, url, linkType) VALUES
    (2, 'External link', 'http://example.com', 'WEB_LINK'),
    (2, 'Trello Card', 'http://trello.com', 'TRELLO_CARD');

INSERT INTO activity (docId, userId, activityType, arguments, timestamp) VALUES
    (2, 1, 'CREATED', NULL, '2017-02-16 11:30');

INSERT INTO attachment (docId, name, size, mimeType) VALUES
    (2, 'scan-1234.pdf', 12000, 'application/pdf');
