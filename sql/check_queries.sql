-- =====================================================================
-- 1차 DB 검사 — 각 테스트 실행 후 MySQL Workbench 에서 확인할 쿼리
--   (각 테스트도 실행이 끝나면 같은 쿼리를 콘솔에 출력함)
--   PK 는 initDB() 직후 기준: maker01 = user_id 2, 프로젝트 1~8, test02 가 만드는 프로젝트 = 9
-- =====================================================================
USE crowdfunding;

-- [initDB] 사전 데이터 개수
SELECT (SELECT COUNT(*) FROM `user`) AS users, (SELECT COUNT(*) FROM project) AS projects,
       (SELECT COUNT(*) FROM reward) AS rewards, (SELECT COUNT(*) FROM funding) AS fundings,
       (SELECT COUNT(*) FROM project_like) AS likes, (SELECT COUNT(*) FROM review) AS reviews;

-- [test01] 메이커(maker01)의 프로젝트별 총 펀딩 금액 / 리워드별 신청·잔여 수량 / 서포터 배송지
SELECT p.project_id, p.project_name, SUM(r.reward_price) AS total_amount, COUNT(*) AS supporters
FROM project p
JOIN funding f ON f.project_id = p.project_id
JOIN reward  r ON r.reward_id  = f.reward_id
WHERE p.user_id = 2
GROUP BY p.project_id;

SELECT reward_id, project_id, reward_name, reward_amount, applied_count,
       reward_amount - applied_count AS remaining
FROM reward WHERE project_id IN (1, 2, 3);

SELECT f.project_id, f.reward_id, u.user_name, s.delivery_address, f.funded_date
FROM funding f
JOIN supporter s ON s.user_id = f.user_id
JOIN `user`    u ON u.user_id = f.user_id
WHERE f.project_id IN (1, 2, 3)
ORDER BY f.project_id, f.reward_id;

-- [test02] 새 프로젝트 — approval_status = 'draft'
SELECT * FROM project ORDER BY project_id DESC LIMIT 1;

-- [test03] 리워드 2개 등록 + approval_status = 'waiting'
SELECT * FROM reward WHERE project_id = 9;
SELECT project_id, project_name, approval_status FROM project WHERE project_id = 9;

-- [test04] 승인 — approval_status = 'approved', project_status = 'in_progress'
SELECT project_id, project_name, approval_status, project_status FROM project WHERE project_id = 9;

-- [test05] sup08 개인정보 (user + supporter 두 테이블)
SELECT u.user_id, u.id, u.pw, u.user_name, u.phone_number, s.delivery_address, s.birth_date
FROM `user` u JOIN supporter s ON s.user_id = u.user_id
WHERE u.id = 'sup08';

-- [test06] 서포터에게 보이는 프로젝트 (승인된 것만)
SELECT project_id, project_name, approval_status, project_status, end_date
FROM project WHERE approval_status = 'approved' ORDER BY created_at DESC;

-- [test07] 필터 결과 비교용
SELECT project_id, project_name, category_id, project_status, end_date
FROM project WHERE approval_status = 'approved' ORDER BY category_id, project_status;

-- [test08] sup01(user_id 4)의 4번 프로젝트 후원 기록 1건 + 7번 리워드 applied_count +1
SELECT * FROM funding WHERE project_id = 4;
SELECT reward_id, reward_name, reward_amount, applied_count FROM reward WHERE project_id = 4;

-- [test09] 2번 프로젝트 리뷰 (리워드명 / 별점 / 리뷰)
SELECT rv.review_id, u.user_name, r.reward_name, rv.rating, rv.review_content, rv.review_at
FROM review rv
JOIN funding f ON f.user_id = rv.user_id AND f.project_id = rv.project_id
JOIN reward  r ON r.reward_id = f.reward_id
JOIN `user`  u ON u.user_id = rv.user_id
WHERE rv.project_id = 2
ORDER BY rv.review_at DESC;

-- [test10] 1번 리워드: applied_count 2 → 3 (sup04 성공), sup05 는 기록 없음 + 수량 그대로
SELECT reward_id, reward_name, reward_amount, applied_count FROM reward WHERE reward_id = 1;
SELECT f.*, u.id FROM funding f JOIN `user` u ON u.user_id = f.user_id WHERE f.reward_id = 1;

-- [공통] 모든 리워드의 applied_count 와 실제 후원 기록 수가 같은지 (consistent = 1 이어야 함)
SELECT r.reward_id, r.reward_amount, r.applied_count, COUNT(f.user_id) AS funding_rows,
       r.applied_count = COUNT(f.user_id) AS consistent
FROM reward r LEFT JOIN funding f ON f.reward_id = r.reward_id
GROUP BY r.reward_id;
