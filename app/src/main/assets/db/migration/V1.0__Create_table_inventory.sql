CREATE TABLE Inventory (
  hash    BINARY(32) NOT NULL PRIMARY KEY,
  stream  INTEGER    NOT NULL,
  expires INTEGER    NOT NULL,
  data    BLOB       NOT NULL,
  type    INTEGER    NOT NULL,
  version INTEGER    NOT NULL
);