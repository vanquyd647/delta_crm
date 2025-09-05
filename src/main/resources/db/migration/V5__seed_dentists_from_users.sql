-- Insert dentist rows for existing users with role = 'DENTIST'
INSERT INTO dentists (name, user_id, specialization, email, phone, active, bio, created_at, updated_at)
SELECT
  COALESCE(full_name, username) AS name,
  id AS user_id,
  NULL AS specialization,
  email,
  NULL AS phone,
  1 AS active,
  NULL AS bio,
  CURRENT_TIMESTAMP AS created_at,
  CURRENT_TIMESTAMP AS updated_at
FROM users u
WHERE u.role = 'DENTIST'
  AND NOT EXISTS (SELECT 1 FROM dentists d WHERE d.user_id = u.id);

