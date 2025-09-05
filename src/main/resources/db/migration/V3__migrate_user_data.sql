-- Backfill user_profiles and user_preferences from existing users table
-- Run after V2 (create tables)

INSERT INTO user_profiles (user_id, created_at, updated_at)
SELECT u.id, NOW(), NOW()
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM user_profiles p WHERE p.user_id = u.id);

INSERT INTO user_preferences (user_id, theme_preference, language_preference, notification_preference, created_at, updated_at)
SELECT u.id,
       COALESCE(u.theme_preference, 'light'),
       COALESCE(u.language_preference, 'vi'),
       COALESCE(u.notification_preference, 'EMAIL'),
       NOW(), NOW()
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM user_preferences p WHERE p.user_id = u.id);

