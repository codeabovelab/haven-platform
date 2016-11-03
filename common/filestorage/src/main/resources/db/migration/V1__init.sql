-- simply key-value storage
CREATE TABLE file_attribute (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  file_id binary(16) NOT NULL,
  name varchar(255) NOT NULL,
  data varchar(255) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY UK_file_attribute__file_id__name (file_id, name)
);
