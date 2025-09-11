-- Migrate appointments.dentist_id values that reference dentists.id -> replace with dentists.user_id
-- Run once. Back up DB before applying.
BEGIN;

-- Preview rows that would be updated (optional):
-- SELECT a.id AS appt_id, a.dentist_id AS old_dentist_id, d.id AS dentist_tbl_id, d.user_id AS dentist_user_id
-- FROM appointments a
-- JOIN dentists d ON a.dentist_id = d.id
-- WHERE d.user_id IS NOT NULL;

-- Update appointments: if current dentist_id points to dentists.id, replace with dentists.user_id
UPDATE appointments a
SET dentist_id = d.user_id
FROM dentists d
WHERE a.dentist_id = d.id
  AND d.user_id IS NOT NULL
  AND a.dentist_id <> d.user_id;

COMMIT;

-- After running: verify with
-- SELECT a.id, a.dentist_id, u.id AS user_id, u.username
-- FROM appointments a
-- LEFT JOIN users u ON a.dentist_id = u.id
-- LIMIT 20;

