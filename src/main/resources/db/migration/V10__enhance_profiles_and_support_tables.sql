-- Add new support tables and extend user_profiles, appointments for customer/staff features

-- Create basic lookup tables
CREATE TABLE IF NOT EXISTS customer_groups (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  notes VARCHAR(1024)
);

CREATE TABLE IF NOT EXISTS sources (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  notes VARCHAR(1024)
);

CREATE TABLE IF NOT EXISTS branches (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  code VARCHAR(64),
  address VARCHAR(512)
);

CREATE TABLE IF NOT EXISTS nationalities (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL
);

CREATE TABLE IF NOT EXISTS occupations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL
);

CREATE TABLE IF NOT EXISTS departments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  notes VARCHAR(1024)
);

-- Suppliers (Labo)
CREATE TABLE IF NOT EXISTS suppliers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64),
  name VARCHAR(255) NOT NULL,
  phone VARCHAR(64),
  representative VARCHAR(255),
  bank_code VARCHAR(64),
  bank_account VARCHAR(128),
  deposit DECIMAL(14,2),
  email VARCHAR(255),
  labo_template VARCHAR(1024),
  address VARCHAR(512)
);

-- Discounts (simple table for staff giving discounts)
CREATE TABLE IF NOT EXISTS discounts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128),
  percent INT,
  amount DECIMAL(14,2),
  created_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (created_by) REFERENCES users(id)
);

-- User groups (role-use groups)
CREATE TABLE IF NOT EXISTS user_groups (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  parent_group_id BIGINT,
  notes VARCHAR(1024),
  time_off VARCHAR(128),
  mandatory_password_change_days INT,
  notify_group BOOLEAN DEFAULT FALSE,
  FOREIGN KEY (parent_group_id) REFERENCES user_groups(id)
);

CREATE TABLE IF NOT EXISTS user_group_members (
  user_group_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  PRIMARY KEY (user_group_id, user_id),
  FOREIGN KEY (user_group_id) REFERENCES user_groups(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Staff profiles (extra fields for employees)
CREATE TABLE IF NOT EXISTS staff_profiles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  code VARCHAR(64),
  nickname VARCHAR(128),
  company_email VARCHAR(255),
  department_id BIGINT,
  branch_id BIGINT,
  birth_date DATE,
  gender VARCHAR(16),
  phone VARCHAR(32),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (department_id) REFERENCES departments(id),
  FOREIGN KEY (branch_id) REFERENCES branches(id)
);

-- Prescriptions
CREATE TABLE IF NOT EXISTS prescriptions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  appointment_id BIGINT,
  patient_id BIGINT,
  doctor_id BIGINT,
  content TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (appointment_id) REFERENCES appointments(id),
  FOREIGN KEY (patient_id) REFERENCES users(id),
  FOREIGN KEY (doctor_id) REFERENCES users(id)
);

-- Extend appointments
ALTER TABLE appointments
  ADD COLUMN IF NOT EXISTS assistant_id BIGINT,
  ADD COLUMN IF NOT EXISTS branch_id BIGINT,
  ADD COLUMN IF NOT EXISTS estimated_minutes INT;

ALTER TABLE appointments
  ADD CONSTRAINT IF NOT EXISTS fk_appointments_assistant FOREIGN KEY (assistant_id) REFERENCES users(id),
  ADD CONSTRAINT IF NOT EXISTS fk_appointments_branch FOREIGN KEY (branch_id) REFERENCES branches(id);

-- Extend user_profiles
ALTER TABLE user_profiles
  ADD COLUMN IF NOT EXISTS source_id BIGINT,
  ADD COLUMN IF NOT EXISTS source_detail VARCHAR(255),
  ADD COLUMN IF NOT EXISTS branch_id BIGINT,
  ADD COLUMN IF NOT EXISTS nationality_id BIGINT,
  ADD COLUMN IF NOT EXISTS occupation_id BIGINT,
  ADD COLUMN IF NOT EXISTS province VARCHAR(128),
  ADD COLUMN IF NOT EXISTS district VARCHAR(128),
  ADD COLUMN IF NOT EXISTS ward VARCHAR(128),
  ADD COLUMN IF NOT EXISTS is_returning BOOLEAN DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS referrer_id BIGINT;

ALTER TABLE user_profiles
  ADD CONSTRAINT IF NOT EXISTS fk_user_profiles_source FOREIGN KEY (source_id) REFERENCES sources(id),
  ADD CONSTRAINT IF NOT EXISTS fk_user_profiles_branch FOREIGN KEY (branch_id) REFERENCES branches(id),
  ADD CONSTRAINT IF NOT EXISTS fk_user_profiles_nationality FOREIGN KEY (nationality_id) REFERENCES nationalities(id),
  ADD CONSTRAINT IF NOT EXISTS fk_user_profiles_occupation FOREIGN KEY (occupation_id) REFERENCES occupations(id),
  ADD CONSTRAINT IF NOT EXISTS fk_user_profiles_referrer FOREIGN KEY (referrer_id) REFERENCES users(id);

-- Many-to-many: user_profiles <-> customer_groups
CREATE TABLE IF NOT EXISTS user_profile_customer_groups (
  user_profile_id BIGINT NOT NULL,
  customer_group_id BIGINT NOT NULL,
  PRIMARY KEY (user_profile_id, customer_group_id),
  FOREIGN KEY (user_profile_id) REFERENCES user_profiles(id),
  FOREIGN KEY (customer_group_id) REFERENCES customer_groups(id)
);

-- Seed a default branch if none exists
INSERT INTO branches (name, code, address) SELECT 'Main Branch', 'MAIN', 'Primary branch' WHERE NOT EXISTS (SELECT 1 FROM branches WHERE code='MAIN');


-- Ensure migration completes
COMMIT;

