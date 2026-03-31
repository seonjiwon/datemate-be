-- 개발용 테스트 유저 (id=1)
-- 이미 존재하면 무시 (IGNORE)
INSERT IGNORE INTO member (member_id, email, nickname, profile_image_url, auth_provider, social_id, role, is_deleted, created_at, updated_at)
VALUES (1, 'dev@datemate.com', '개발테스터', NULL, 'KAKAO', 'dev_kakao_001', 'USER', false, NOW(), NOW());
