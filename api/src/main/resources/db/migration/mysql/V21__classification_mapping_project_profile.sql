CREATE TABLE mapping_project_profile (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  project_type VARCHAR(50) NOT NULL DEFAULT 'SIMPLE_SNOMED',
  source_system_uri VARCHAR(255),
  source_version VARCHAR(64),
  target_system_uri VARCHAR(255),
  target_version VARCHAR(64),
  mapping_profile VARCHAR(100),
  ai_enabled BIT NOT NULL DEFAULT 0,
  auto_accept_policy_version VARCHAR(64),
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mapping_project_profile_project (project_id),
  CONSTRAINT fk_mapping_project_profile_project
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
);
