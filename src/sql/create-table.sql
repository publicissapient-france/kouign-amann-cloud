DROP TABLE IF EXISTS votes;
CREATE TABLE votes (
  vote_id VARCHAR(100),
  nfc_id VARCHAR(100) NOT NULL,
  rasp_id VARCHAR(100) NOT NULL,
  slot_dt VARCHAR(100) NOT NULL,
  note INTEGER NOT NULL,
  dt DATETIME NOT NULL,
  PRIMARY KEY (vote_id)
);


DROP TABLE IF EXISTS rasp_slot;
CREATE TABLE rasp_slot (
  rasp_id VARCHAR(100),
  slot_dt VARCHAR(100),
  slot_id VARCHAR(100),
  PRIMARY KEY (rasp_id, slot_dt)
);

DROP TABLE IF EXISTS devoxxian;
CREATE TABLE devoxxian (
  mail VARCHAR(100) NOT NULL,
  name VARCHAR(100),
  twitter VARCHAR(100),
  postalCode VARCHAR(100),
  company VARCHAR(100),
  comment VARCHAR(1000),
  PRIMARY KEY (mail)
);

INSERT INTO rasp_slot VALUES ("Salle-1","2014-02-03-12", 1);
INSERT INTO rasp_slot VALUES ("Salle-1","2014-02-03-13", 2);
INSERT INTO rasp_slot VALUES ("Salle-1","2014-02-03-14", 3);
INSERT INTO rasp_slot VALUES ("Salle-1","2014-02-03-15", 4);
INSERT INTO rasp_slot VALUES ("Salle-1","2014-02-03-16", 5);
INSERT INTO rasp_slot VALUES ("Salle-1","2014-02-03-17", 6);
