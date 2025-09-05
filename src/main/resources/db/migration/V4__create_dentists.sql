-- Create dentists table
CREATE TABLE IF NOT EXISTS dentists (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  user_id BIGINT NULL,
  specialization VARCHAR(128) NULL,
  email VARCHAR(256) NULL,
  phone VARCHAR(64) NULL,
  active TINYINT(1) DEFAULT 1,
  bio VARCHAR(2000) NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_dentist_user_id (user_id)
);

-- Optional FK if users table exists and you want referential integrity (commented out)
-- ALTER TABLE dentists ADD CONSTRAINT fk_dentist_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

