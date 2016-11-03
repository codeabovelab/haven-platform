CREATE TABLE token (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  creation_date datetime NOT NULL,
  token varchar(255) NOT NULL,
  user_name varchar(255) NOT NULL,
  device_hash varchar(255) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY UK_token_token (token)
) ;
CREATE TABLE acl_class (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  class varchar(100) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY UK_class (class)
) ;


CREATE TABLE acl_sid (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  principal bit(1) NOT NULL,
  sid varchar(255) DEFAULT NULL,
  tenant bigint(20) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uc_aclsid_tenant_sid_principal (tenant,sid,principal)
) ;

CREATE TABLE acl_object_identity (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  entries_inheriting bit(1) DEFAULT NULL,
  object_id_identity bigint(20) DEFAULT NULL,
  object_id_class bigint(20) DEFAULT NULL,
  owner_sid bigint(20) DEFAULT NULL,
  parent_object bigint(20) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uc_aclobjectidentity_class__identity (object_id_class,object_id_identity),
  CONSTRAINT FK_object_class FOREIGN KEY (object_id_class) REFERENCES acl_class (id),
  CONSTRAINT FK_parent_class FOREIGN KEY (parent_object) REFERENCES acl_object_identity (id),
  CONSTRAINT FK_owner FOREIGN KEY (owner_sid) REFERENCES acl_sid (id)
) ;


CREATE TABLE acl_entry (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  audit_failure bit(1) NOT NULL,
  audit_success bit(1) NOT NULL,
  granting bit(1) NOT NULL,
  mask int(11) NOT NULL,
  ace_order int(11) NOT NULL,
  acl_object_identity bigint(20) DEFAULT NULL,
  sid bigint(20) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uc_aclentry_objectidentity_order (acl_object_identity,ace_order),
  CONSTRAINT FK_acl_object_identity FOREIGN KEY (acl_object_identity) REFERENCES acl_object_identity (id),
  CONSTRAINT FK_acl_sid FOREIGN KEY (sid) REFERENCES acl_sid (id)
) ;

CREATE TABLE acl_object_identity_entries (
  acl_object_identity_id bigint(20) NOT NULL,
  entries_id bigint(20) NOT NULL,
  UNIQUE KEY FK_entries (entries_id),
  CONSTRAINT FK_acl_object_identity_id FOREIGN KEY (acl_object_identity_id) REFERENCES acl_object_identity (id),
  CONSTRAINT FK_entries_id FOREIGN KEY (entries_id) REFERENCES acl_entry (id)
) ;


CREATE TABLE tenants (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  name varchar(255) DEFAULT NULL,
  root bit(1) NOT NULL,
  parent_id bigint(20) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT FK_parent FOREIGN KEY (parent_id) REFERENCES tenants (id)
) ;

CREATE TABLE authority (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  role varchar(255) NOT NULL,
  tenant_id bigint(20) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY UK_authority (role, tenant_id),
  CONSTRAINT FK_tenant_id FOREIGN KEY (tenant_id) REFERENCES tenants (id)
) ;

CREATE TABLE user_auth_details (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  account_non_expired bit(1) NOT NULL,
  account_non_locked bit(1) NOT NULL,
  credentials_non_expired bit(1) NOT NULL,
  email varchar(100),
  enabled bit(1) NOT NULL,
  mobile bigint(20) NOT NULL,
  password varchar(100) DEFAULT NULL,
  username varchar(100) NOT NULL,
  tenant_id bigint(20) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY UK_email (email),
  UNIQUE KEY UK_mobile (mobile),
  UNIQUE KEY UK_username (username),
  CONSTRAINT FK_tenant_id_user_auth_details FOREIGN KEY (tenant_id) REFERENCES tenants (id)
) ;

CREATE TABLE user_attributes (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  user_auth_details_id bigint(20) NOT NULL,
  email_actived   BIT(1) NOT NULL,
  mobile_verified BIT(1) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT FK_user_additional_details_id FOREIGN KEY (user_auth_details_id) REFERENCES user_auth_details (id)
);

CREATE TABLE user_auth_details_authorities (
  user_auth_details_id bigint(20) NOT NULL,
  authorities_id bigint(20) NOT NULL,
  PRIMARY KEY (user_auth_details_id,authorities_id),
  CONSTRAINT FK_user_auth_details_id FOREIGN KEY (user_auth_details_id) REFERENCES user_auth_details (id),
  CONSTRAINT FK_authorities_id FOREIGN KEY (authorities_id) REFERENCES authority (id)
) ;

CREATE TABLE authority_group (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  tenant_id bigint(20) DEFAULT NULL,
  name varchar(255) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY UK_authority_group__name_tenant (name, tenant_id),
  CONSTRAINT FK_authority_group__tenant_id FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);

CREATE TABLE authority_group_authorities (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  group_id bigint(20) NOT NULL,
  authority_id bigint(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY UK_authority_group_authorities__group_authority (group_id, authority_id),
  CONSTRAINT FK_authority_group_authorities__group_id FOREIGN KEY (group_id) REFERENCES authority_group (id),
  CONSTRAINT FK_authority_group_authorities__authority_id FOREIGN KEY (authority_id) REFERENCES authority (id)
);

CREATE TABLE user_authority_groups (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  group_id bigint(20) NOT NULL,
  user_id bigint(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY UK_user_authority_groups__user_group (group_id, user_id),
  CONSTRAINT FK_user_authority_groups__group_id FOREIGN KEY (group_id) REFERENCES authority_group (id),
  CONSTRAINT FK_user_authority_groups__user_id FOREIGN KEY (user_id) REFERENCES user_auth_details (id)
);

-- simply key-value storage for user
CREATE TABLE user_props (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  user_auth_details_id bigint(20) NOT NULL,
  name varchar(255) NOT NULL,
  data varchar(255) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY UK_user_props__user__name (user_auth_details_id, name),
  CONSTRAINT FK_user_props__user_auth_details__id FOREIGN KEY (user_auth_details_id) REFERENCES user_auth_details (id)
);


-- initial data

INSERT INTO tenants
    (id,          name, root, parent_id) VALUES
    ( 1, 'root-tenant',    1,      null);

--
INSERT INTO authority
    (id,          role, tenant_id) VALUES
    ( 1,  'ROLE_ADMIN',      null);

INSERT INTO authority
    (id,          role, tenant_id) VALUES
    ( 2,   'ROLE_USER',      null);

--
INSERT INTO user_auth_details
   (id, account_non_expired, account_non_locked, credentials_non_expired,              email, enabled, mobile,     password,  username, tenant_id) VALUES
   ( 1,                   1,                  1,                       1, 'admin@localhost.',       1,      0,   '$2a$10$4t0xLFEqGzwJfPzbUen.GeRnXcoXynkmIzhLhHpppHcDiW5gWV1Dq',   'admin',         1);

INSERT INTO user_auth_details_authorities
   (user_auth_details_id, authorities_id) VALUES
   (                   1,              1);

--
INSERT INTO user_auth_details
   (id, account_non_expired, account_non_locked, credentials_non_expired,              email, enabled, mobile,     password,  username, tenant_id) VALUES
   ( 2,                   1,                  1,                       1,  'user@localhost.',       1,      1,   '$2a$10$4t0xLFEqGzwJfPzbUen.GeRnXcoXynkmIzhLhHpppHcDiW5gWV1Dq',   'user',         1);

INSERT INTO user_auth_details_authorities
   (user_auth_details_id, authorities_id) VALUES
   (                   2,              2);

