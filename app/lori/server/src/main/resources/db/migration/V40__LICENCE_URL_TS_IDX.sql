ALTER TABLE item_metadata ADD COLUMN ts_licence_url tsvector GENERATED ALWAYS AS (to_tsvector('english', licence_url)) STORED;
CREATE INDEX ts_licence_url_idx ON item_metadata USING GIN (ts_licence_url);