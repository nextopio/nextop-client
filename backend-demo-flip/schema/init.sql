
-- feed
CREATE TABLE Feed(feed_id CHAR(64) NOT NULL,
  flip_id CHAR(64) NOT NULL,
  most_recent_update_index BIGINT NOT NULL DEFAULT 0,
  deleted BOOLEAN NOT NULL DEFAULT false,
  PRIMARY KEY(feed_id, flip_id));
CREATE INDEX FeedByMostRecentUpdate ON Feed(feed_id, most_recent_update_index, flip_id);

CREATE TABLE FeedUpdates(feed_id CHAR(64) NOT NULL,
  update_index BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY(feed_id));


--  flip info
CREATE TABLE FlipInfo(flip_id CHAR(64) NOT NULL,
  intro VARCHAR(512) NULL DEFAULT NULL,
  most_recent_update_index BIGINT DEFAULT 0,
  deleted BOOLEAN DEFAULT false,
  PRIMARY KEY(flip_id));


-- flip frame
CREATE TABLE FlipFrame(flip_id CHAR(64) NOT NULL,
  frame_id CHAR(64) NOT NULL,
  creation_time BIGINT NULL DEFAULT NULL,
  image_url VARCHAR(512) NULL DEFAULT NULL,
  most_recent_update_index BIGINT DEFAULT 0,
  deleted BOOLEAN DEFAULT false,
  PRIMARY KEY(flip_id, frame_id));
CREATE INDEX FlipFrameByMostRecentUpdate ON FlipFrame(flip_id, most_recent_update_index, frame_id, creation_time);

CREATE TABLE FlipFrameUpdates(flip_id CHAR(64) NOT NULL,
  update_index BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY(flip_id));

