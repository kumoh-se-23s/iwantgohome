-- =====================================================================
-- 검사 당일 Workbench 에 미리 띄워 둘 확인 쿼리 (검사 순서대로)
--   각 테스트를 실행한 뒤 해당 번호의 쿼리를 실행해서 테이블 변화를 보여 준다.
--   번호(PK)는 initDB 기준 기본값. 테스트 상수를 바꿨다면 여기 숫자도 같이 바꾼다.
-- =====================================================================
USE crowdfunding;

-- [0] initDB 결과: 테이블별 행 수 (사용자 11 / 프로젝트 8 / 리워드 16 / 후원 16 / 좋아요 12 / 리뷰 5)
SELECT (SELECT COUNT(*) FROM `user`) AS users, (SELECT COUNT(*) FROM project) AS projects,
       (SELECT COUNT(*) FROM reward) AS rewards, (SELECT COUNT(*) FROM funding) AS fundings,
       (SELECT COUNT(*) FROM project_like) AS likes, (SELECT COUNT(*) FROM review) AS reviews;

-- [1] maker01(user 2) 의 프로젝트별 총 펀딩 금액
SELECT f.project_id, SUM(r.reward_price) AS total_amount, COUNT(*) AS supporters
  FROM funding f
  JOIN reward r  ON r.reward_id = f.reward_id
  JOIN project p ON p.project_id = f.project_id
 WHERE p.user_id = 2
 GROUP BY f.project_id;

-- [1] 리워드별 신청 수량 / 잔여 수량 / 서포터 / 배송지
SELECT r.project_id, r.reward_id, r.reward_name, r.applied_count, r.reward_amount,
       r.reward_amount - r.applied_count AS remaining, u.user_name, s.delivery_address
  FROM reward r
  JOIN project p        ON p.project_id = r.project_id
  LEFT JOIN funding f   ON f.reward_id = r.reward_id
  LEFT JOIN `user` u    ON u.user_id = f.user_id
  LEFT JOIN supporter s ON s.user_id = f.user_id
 WHERE p.user_id = 2 AND p.approval_status = 'approved'
 ORDER BY r.project_id, r.reward_id;

-- [2] 새 프로젝트(9번)가 'draft' 로 생겼는지
SELECT * FROM project ORDER BY project_id DESC LIMIT 1;

-- [3] 9번 프로젝트의 리워드 2개 + 승인 상태 'waiting'
SELECT * FROM reward WHERE project_id = 9;
SELECT project_id, project_name, approval_status, project_status FROM project WHERE project_id = 9;

-- [4] 9번 프로젝트 'approved' + 'in_progress'
SELECT project_id, project_name, approval_status, project_status FROM project WHERE project_id = 9;

-- [5] sup08(user 11) 의 이름 · 연락처 · 비밀번호 · 배송지 · 생년월일 (user + supporter 두 테이블)
SELECT u.user_id, u.user_name, u.phone_number, u.pw, s.delivery_address, s.birth_date
  FROM `user` u JOIN supporter s ON s.user_id = u.user_id
 WHERE u.user_id = 11;

-- [6] 서포터에게 보이는 프로젝트 = 승인된 것만 (승인 대기 6번, 거절 7번은 안 보여야 함)
SELECT project_id, project_name, approval_status, project_status, end_date
  FROM project
 ORDER BY created_at DESC;

-- [7] 필터: 카테고리 1 + 펀딩중
SELECT project_id, project_name, category_id, project_status, end_date
  FROM project
 WHERE approval_status = 'approved' AND category_id = 1
   AND project_status = 'in_progress' AND end_date >= CURDATE()
 ORDER BY created_at DESC;

-- [8] 4번 프로젝트: 8번 리워드 수량이 1 → 2 로 늘었는지 (잔여 9 → 8), sup01(user 4) 후원 기록, 메이커가 볼 후원자 목록
SELECT reward_id, reward_name, reward_amount, applied_count, reward_amount - applied_count AS remaining
  FROM reward WHERE project_id = 4;
SELECT f.*, u.user_name, s.delivery_address
  FROM funding f
  JOIN `user` u    ON u.user_id = f.user_id
  JOIN supporter s ON s.user_id = f.user_id
 WHERE f.project_id = 4;

-- [9] 2번 프로젝트에 sup01 리뷰가 추가됐는지 (리워드명 / 별점 / 리뷰), 3번(실패)에는 추가되지 않았는지
SELECT v.review_id, p.project_name, r.reward_name, v.rating, v.review_content, v.review_at
  FROM review v
  JOIN funding f ON f.user_id = v.user_id AND f.project_id = v.project_id
  JOIN reward r  ON r.reward_id = f.reward_id
  JOIN project p ON p.project_id = v.project_id
 WHERE v.project_id IN (2, 3)
 ORDER BY v.review_at DESC;

-- [10] 1번 리워드: 실행 전 applied_count 2 → 실행 후 3 (= 수량 3, 잔여 0)
--      후원 기록: sup05(user 8) 는 생기고, sup06(user 9) 는 없어야 함
SELECT reward_id, reward_name, reward_amount, applied_count FROM reward WHERE reward_id = 1;
SELECT * FROM funding WHERE reward_id = 1 ORDER BY funded_date;

-- [공통] 모든 리워드에서 applied_count 와 실제 후원 행 수가 같은지 (결과가 0행이면 정상)
SELECT r.reward_id, r.applied_count, COUNT(f.user_id) AS real_count
  FROM reward r LEFT JOIN funding f ON f.reward_id = r.reward_id
 GROUP BY r.reward_id, r.applied_count
HAVING r.applied_count <> COUNT(f.user_id);
