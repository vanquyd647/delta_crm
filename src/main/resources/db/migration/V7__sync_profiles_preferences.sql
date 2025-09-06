-- V7: Backfill and synchronize user_profiles and user_preferences from users table
-- Safe to run multiple times

-- 1) Create missing user_profiles (copy avatar_url from users)
INSERT INTO user_profiles (user_id, avatar_url, created_at, updated_at)
SELECT u.id, u.avatar_url, NOW(), NOW()
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM user_profiles p WHERE p.user_id = u.id);

-- 2) Update existing profiles' avatar_url from users where empty
UPDATE user_profiles p
JOIN users u ON p.user_id = u.id
SET p.avatar_url = u.avatar_url
WHERE (p.avatar_url IS NULL OR p.avatar_url = '')
  AND (u.avatar_url IS NOT NULL AND u.avatar_url <> '');

-- 3) Create missing user_preferences with sensible defaults
INSERT INTO user_preferences (user_id, theme_preference, language_preference, notification_preference, created_at, updated_at)
SELECT u.id,
       COALESCE(u.theme_preference, 'light'),
       COALESCE(u.language_preference, 'vi'),
       COALESCE(u.notification_preference, 'EMAIL'),
       NOW(), NOW()
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM user_preferences p WHERE p.user_id = u.id);

-- 4) Optional: synchronize preference columns from users into user_preferences when present
UPDATE user_preferences p
JOIN users u ON p.user_id = u.id
SET p.theme_preference = COALESCE(u.theme_preference, p.theme_preference),
    p.language_preference = COALESCE(u.language_preference, p.language_preference),
    p.notification_preference = COALESCE(u.notification_preference, p.notification_preference),
    p.updated_at = NOW()
WHERE (u.theme_preference IS NOT NULL OR u.language_preference IS NOT NULL OR u.notification_preference IS NOT NULL);

