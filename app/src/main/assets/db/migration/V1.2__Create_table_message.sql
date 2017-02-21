CREATE TABLE Message (
  id                      INTEGER PRIMARY KEY AUTOINCREMENT,
  iv                      BINARY(32)    UNIQUE,
  type                    VARCHAR(20)   NOT NULL,
  sender                  VARCHAR(40)   NOT NULL,
  recipient               VARCHAR(40),
  data                    BLOB          NOT NULL,
  sent                    INTEGER,
  received                INTEGER,
  status                  VARCHAR(20)   NOT NULL,
  initial_hash            BINARY(64)    UNIQUE,

  FOREIGN KEY (sender)    REFERENCES Address (address),
  FOREIGN KEY (recipient) REFERENCES Address (address)
);

CREATE TABLE Label (
  id                      INTEGER PRIMARY KEY AUTOINCREMENT,
  label                   VARCHAR(255)  NOT NULL,
  type                    VARCHAR(20),
  color                   INT NOT NULL DEFAULT 4278190080, -- FF000000
  ord                     INTEGER,

  CONSTRAINT UC_label UNIQUE (label),
  CONSTRAINT UC_order UNIQUE (ord)
);

CREATE TABLE Message_Label (
  message_id INTEGER NOT NULL,
  label_id   INTEGER NOT NULL,

  PRIMARY KEY (message_id, label_id),
  FOREIGN KEY (message_id) REFERENCES Message (id),
  FOREIGN KEY (label_id)   REFERENCES Label (id)
);

INSERT INTO Label(label, type, color, ord) VALUES ('Inbox', 'INBOX', 4278190335, 0);
INSERT INTO Label(label, type, color, ord) VALUES ('Drafts', 'DRAFT', 4294940928, 10);
INSERT INTO Label(label, type, color, ord) VALUES ('Sent', 'SENT', 4294967040, 20);
INSERT INTO Label(label, type, ord) VALUES ('Broadcast', 'BROADCAST', 50);
INSERT INTO Label(label, type, ord) VALUES ('Unread', 'UNREAD', 90);
INSERT INTO Label(label, type, ord) VALUES ('Trash', 'TRASH', 100);
