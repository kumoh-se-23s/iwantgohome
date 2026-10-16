-- =====================================================================
-- seed.sql : DB 파트 검사(11/2)용 사전 데이터
--   - DataInitializer.initDB() 가 실행한다. (Workbench 에서 직접 실행해도 된다)
--   - 맨 앞에서 모든 테이블을 비우므로 몇 번을 실행해도 PK 가 항상 같다.
--   - 날짜는 실행한 날(@today) 기준 상대값 → 언제 실행해도 '펀딩중 / 마감·성공 / 마감·실패' 가 생긴다.
--   - crowdfunding.sql 의 CHECK 제약(아이디 5~10자, 비밀번호 8~16자, 연락처 형식 등)을 모두 지킨다.
--   - 문자열 안에 세미콜론을 쓰지 말 것 (줄 끝 세미콜론으로 문장을 나눠서 실행한다)
--
-- [ID 맵] 테스트 클래스 맨 위 상수는 모두 이 번호 기준
--   사용자   1 admin01(관리자)  2 maker01 김민수  3 maker02 이수진  4~11 sup01~sup08 (서포터)
--   카테고리 1 테크·가전  2 푸드
--   프로젝트 1 키보드(maker01, 펀딩중)        2 그래놀라(maker01, 마감·성공)  3 빔프로젝터(maker01, 마감·실패)
--            4 감귤주스(maker02, 펀딩중)      5 식물재배기(maker02, 펀딩중·마감임박)
--            6 비건쿠키(maker02, 승인 대기)   7 발효식초(maker01, 승인 거절)  8 수면이어폰(maker02, 마감·성공)
--            → test02 에서 새로 등록하는 프로젝트는 9번이 된다
--   검사용   test08 : sup01 → 4번 프로젝트 8번 리워드 (한정 10개 중 1개 신청됨)
--            test09 : sup01 → 2번 프로젝트 리뷰 (마감·성공, sup01 후원)  /  실패 확인: sup04 → 3번(마감·실패)
--            test10 : 1번 프로젝트 1번 리워드 (한정 3개 중 2개 신청됨 → 잔여 1개)  sup05 성공 / sup06 실패
--            test10b: 5번 프로젝트 9번 리워드 (한정 2개, 신청 0개) 에 6명 동시 신청 → 정확히 2명 성공
-- =====================================================================

SET @today = DATE(IFNULL(@today, CURDATE()));

-- ---------------------------------------------------------------------
-- 0. 전체 비우기 (FK 순서와 상관없이 비우기 위해 잠시 FK 검사를 끈다)
-- ---------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE review;
TRUNCATE TABLE project_like;
TRUNCATE TABLE funding;
TRUNCATE TABLE reward;
TRUNCATE TABLE project;
TRUNCATE TABLE category;
TRUNCATE TABLE supporter;
TRUNCATE TABLE `user`;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- 1. 사용자 (메이커 · 서포터 · 관리자 계정은 사전 생성)
-- ---------------------------------------------------------------------
INSERT INTO `user` (user_id, id, pw, role, user_name, phone_number) VALUES
  ( 1, 'admin01', 'admin1234', 'admin',     '관리자', '010-1000-0001'),
  ( 2, 'maker01', 'maker1234', 'maker',     '김민수', '010-2000-0001'),
  ( 3, 'maker02', 'maker1234', 'maker',     '이수진', '010-2000-0002'),
  ( 4, 'sup01',   'pass1234',  'supporter', '박준호', '010-3000-0001'),
  ( 5, 'sup02',   'pass1234',  'supporter', '최유나', '010-3000-0002'),
  ( 6, 'sup03',   'pass1234',  'supporter', '정하늘', '010-3000-0003'),
  ( 7, 'sup04',   'pass1234',  'supporter', '강도윤', '010-3000-0004'),
  ( 8, 'sup05',   'pass1234',  'supporter', '윤서연', '010-3000-0005'),
  ( 9, 'sup06',   'pass1234',  'supporter', '장민재', '010-3000-0006'),
  (10, 'sup07',   'pass1234',  'supporter', '임채원', '010-3000-0007'),
  (11, 'sup08',   'pass1234',  'supporter', '한소율', '010-3000-0008');

-- 서포터 프로필 (배송지 · 생년월일)
INSERT INTO supporter (user_id, delivery_address, birth_date) VALUES
  ( 4, '서울특별시 강남구 테헤란로 152, 101동 1203호',   '2003-04-12'),
  ( 5, '부산광역시 해운대구 센텀중앙로 79, 202호',        '2002-11-03'),
  ( 6, '대구광역시 중구 동성로 12, 3층',                  '2004-01-25'),
  ( 7, '인천광역시 연수구 송도과학로 32, 1502호',         '2001-07-19'),
  ( 8, '광주광역시 북구 용봉로 77, 기숙사 B동 315호',     '2003-09-30'),
  ( 9, '대전광역시 유성구 대학로 99, 505호',              NULL),
  (10, '경기도 수원시 영통구 광교로 145, 801호',          '2002-02-14'),
  (11, '경상북도 구미시 구미대로 100, 1203호',             '2004-06-08');

-- ---------------------------------------------------------------------
-- 2. 카테고리 (2개로 제한)
-- ---------------------------------------------------------------------
INSERT INTO category (category_id, category_name) VALUES
  (1, '테크·가전'),
  (2, '푸드');

-- ---------------------------------------------------------------------
-- 3. 프로젝트 8개
--    승인된 프로젝트만 project_status 가 있다 (DB CHECK 제약)
--    성공/실패 프로젝트는 아래 후원 금액 합계가 목표 금액 이상/미만이 되도록 맞춰 두었다
-- ---------------------------------------------------------------------
INSERT INTO project (project_id, project_name, project_description, approval_status, reject_reason,
                     project_status, start_date, end_date, target_amount, created_at, user_id, category_id) VALUES
  (1, '접이식 블루투스 키보드',
      '주머니에 들어가는 3단 접이식 키보드. 기기 3대까지 연결',
      'approved', NULL, 'in_progress',
      @today - INTERVAL 10 DAY, @today + INTERVAL 20 DAY, 1000000,
      TIMESTAMP(@today - INTERVAL 12 DAY, '10:00:00'), 2, 1),
  (2, '수제 그래놀라 정기배송',
      '설탕 대신 꿀로 구운 수제 그래놀라를 매주 배송',
      'approved', NULL, 'success',
      @today - INTERVAL 40 DAY, @today - INTERVAL 10 DAY, 300000,
      TIMESTAMP(@today - INTERVAL 42 DAY, '09:30:00'), 2, 2),
  (3, '휴대용 미니 빔프로젝터',
      '손바닥만 한 1080p 빔프로젝터',
      'approved', NULL, 'fail',
      @today - INTERVAL 50 DAY, @today - INTERVAL 20 DAY, 2000000,
      TIMESTAMP(@today - INTERVAL 52 DAY, '14:00:00'), 2, 1),
  (4, '제주 감귤 착즙 주스',
      '제주 노지 감귤 100퍼센트 착즙, 무첨가',
      'approved', NULL, 'in_progress',
      @today - INTERVAL 5 DAY, @today + INTERVAL 25 DAY, 500000,
      TIMESTAMP(@today - INTERVAL 7 DAY, '11:00:00'), 3, 2),
  (5, '스마트 식물 재배기',
      '물과 빛을 알아서 조절하는 실내 식물 재배기',
      'approved', NULL, 'in_progress',
      @today - INTERVAL 25 DAY, @today + INTERVAL 2 DAY, 800000,
      TIMESTAMP(@today - INTERVAL 27 DAY, '16:00:00'), 3, 1),
  (6, '비건 단백질 쿠키',
      '식물성 단백질 12g 이 들어간 비건 쿠키',
      'waiting', NULL, NULL,
      @today, @today + INTERVAL 30 DAY, 400000,
      TIMESTAMP(@today - INTERVAL 2 DAY, '13:00:00'), 3, 2),
  (7, '전통 발효 식초',
      '3년 숙성 전통 현미 식초',
      'reject', '리워드 제공 시기와 원재료 정보가 부족합니다. 보완 후 다시 신청해 주세요.', NULL,
      @today + INTERVAL 5 DAY, @today + INTERVAL 35 DAY, 300000,
      TIMESTAMP(@today - INTERVAL 4 DAY, '15:00:00'), 2, 2),
  (8, '노이즈캔슬링 수면 이어폰',
      '옆으로 누워도 아프지 않은 초소형 수면 이어폰',
      'approved', NULL, 'success',
      @today - INTERVAL 60 DAY, @today - INTERVAL 30 DAY, 500000,
      TIMESTAMP(@today - INTERVAL 62 DAY, '10:30:00'), 3, 1);

-- ---------------------------------------------------------------------
-- 4. 리워드 16개 (프로젝트마다 2개 이상 + 수량 제한 리워드 1개 이상)
--    reward_amount NULL = 수량 무제한 / 제공 시기 = 펀딩 종료 2주 뒤
--    applied_count 는 0 으로 넣고, 맨 아래에서 실제 후원 건수로 맞춘다
-- ---------------------------------------------------------------------
INSERT INTO reward (reward_id, project_id, reward_name, reward_description, reward_price, reward_amount,
                    applied_count, reward_delivery_date) VALUES
  ( 1, 1, '얼리버드 키보드',        '키보드 1개 (얼리버드 할인)',          39000,    3, 0, @today + INTERVAL 34 DAY),
  ( 2, 1, '키보드 기본',            '키보드 1개',                          49000, NULL, 0, @today + INTERVAL 34 DAY),
  ( 3, 2, '1개월 정기배송',         '그래놀라 500g 4회 배송',              29000, NULL, 0, @today + INTERVAL  4 DAY),
  ( 4, 2, '3개월 정기배송',         '그래놀라 500g 12회 배송',             79000,   20, 0, @today + INTERVAL  4 DAY),
  ( 5, 3, '얼리버드 빔프로젝터',    '빔프로젝터 1대 (얼리버드 할인)',      59000,   50, 0, @today - INTERVAL  6 DAY),
  ( 6, 3, '빔프로젝터 + 거치대',    '빔프로젝터 1대와 삼각 거치대',        69000, NULL, 0, @today - INTERVAL  6 DAY),
  ( 7, 4, '착즙 주스 10병',         '감귤 주스 200ml 10병',                25000, NULL, 0, @today + INTERVAL 39 DAY),
  ( 8, 4, '착즙 주스 30병 세트',    '감귤 주스 200ml 30병 (한정 수량)',    65000,   10, 0, @today + INTERVAL 39 DAY),
  ( 9, 5, '식물 재배기 1대',        '재배기 1대 (한정 수량)',              99000,    2, 0, @today + INTERVAL 16 DAY),
  (10, 5, '식물 재배기 2대 세트',   '재배기 2대',                         189000, NULL, 0, @today + INTERVAL 16 DAY),
  (11, 6, '쿠키 10개입',            '비건 쿠키 10개',                      15000,  100, 0, @today + INTERVAL 44 DAY),
  (12, 6, '쿠키 30개입',            '비건 쿠키 30개',                      39000, NULL, 0, @today + INTERVAL 44 DAY),
  (13, 7, '발효 식초 500ml',        '현미 식초 1병',                       18000,   50, 0, @today + INTERVAL 49 DAY),
  (14, 7, '발효 식초 3병 세트',     '현미 식초 3병',                       49000, NULL, 0, @today + INTERVAL 49 DAY),
  (15, 8, '수면 이어폰',            '이어폰 1세트',                       129000,   30, 0, @today - INTERVAL 16 DAY),
  (16, 8, '수면 이어폰 + 케이스',   '이어폰 1세트와 충전 케이스',          149000, NULL, 0, @today - INTERVAL 16 DAY);

-- ---------------------------------------------------------------------
-- 5. 후원 16건 (PK = 서포터 + 프로젝트 → 프로젝트당 1회)
--    maker01 의 프로젝트 1·2·3 모두 실제 후원 기록이 있다 (검사 조건)
--    2번: 79,000 x 4 + 29,000 = 345,000 ≥ 목표 300,000 → 성공
--    3번: 59,000 + 69,000     = 128,000 < 목표 2,000,000 → 실패
--    8번: 129,000 x 2 + 149,000 x 2 = 556,000 ≥ 목표 500,000 → 성공
-- ---------------------------------------------------------------------
INSERT INTO funding (user_id, project_id, reward_id, funded_date) VALUES
  ( 5, 1,  1, TIMESTAMP(@today - INTERVAL  8 DAY, '10:12:00')),
  ( 6, 1,  1, TIMESTAMP(@today - INTERVAL  6 DAY, '21:40:00')),
  ( 7, 1,  2, TIMESTAMP(@today - INTERVAL  3 DAY, '09:05:00')),
  ( 4, 2,  4, TIMESTAMP(@today - INTERVAL 35 DAY, '08:20:00')),
  ( 5, 2,  4, TIMESTAMP(@today - INTERVAL 33 DAY, '12:45:00')),
  ( 6, 2,  3, TIMESTAMP(@today - INTERVAL 30 DAY, '19:10:00')),
  ( 8, 2,  4, TIMESTAMP(@today - INTERVAL 25 DAY, '22:30:00')),
  ( 9, 2,  4, TIMESTAMP(@today - INTERVAL 15 DAY, '07:55:00')),
  ( 7, 3,  5, TIMESTAMP(@today - INTERVAL 45 DAY, '11:00:00')),
  (10, 3,  6, TIMESTAMP(@today - INTERVAL 30 DAY, '17:25:00')),
  ( 5, 4,  8, TIMESTAMP(@today - INTERVAL  2 DAY, '13:15:00')),
  (11, 5, 10, TIMESTAMP(@today - INTERVAL 10 DAY, '20:00:00')),
  ( 4, 8, 15, TIMESTAMP(@today - INTERVAL 55 DAY, '09:00:00')),
  ( 6, 8, 16, TIMESTAMP(@today - INTERVAL 50 DAY, '14:30:00')),
  ( 7, 8, 15, TIMESTAMP(@today - INTERVAL 45 DAY, '18:40:00')),
  (10, 8, 16, TIMESTAMP(@today - INTERVAL 40 DAY, '23:10:00'));

-- 리워드별 신청 수량(applied_count)을 실제 후원 건수와 맞춘다
-- (WHERE 를 붙인 이유: Workbench 의 Safe Update 모드에서도 실행되도록)
UPDATE reward r
   SET r.applied_count = (SELECT COUNT(*) FROM funding f WHERE f.reward_id = r.reward_id)
 WHERE r.reward_id > 0;

-- ---------------------------------------------------------------------
-- 6. 좋아요 12건 (2차 '좋아요순 정렬' 용)
-- ---------------------------------------------------------------------
INSERT INTO project_like (project_id, user_id) VALUES
  (1,  4), (1,  5), (1,  8),
  (2,  4), (2,  6),
  (4,  4), (4,  7), (4,  9),
  (5,  5), (5, 10),
  (8,  6), (8, 11);

-- ---------------------------------------------------------------------
-- 7. 리뷰 5건 (마감 + 성공한 프로젝트에 후원한 서포터만)
--    sup01(4번)은 2번 프로젝트에 후원했지만 아직 리뷰가 없다 → test09 에서 작성
-- ---------------------------------------------------------------------
INSERT INTO review (review_id, user_id, project_id, rating, review_content, review_at) VALUES
  (1,  5, 2, 5, '매일 아침 잘 먹고 있어요. 견과류가 넉넉해서 좋아요.',  TIMESTAMP(@today - INTERVAL  8 DAY, '08:30:00')),
  (2,  6, 2, 4, '맛은 좋은데 배송 상자가 조금 찌그러져 왔어요.',        TIMESTAMP(@today - INTERVAL  6 DAY, '20:15:00')),
  (3,  6, 8, 5, '옆으로 누워 자도 귀가 아프지 않아서 만족합니다.',      TIMESTAMP(@today - INTERVAL 25 DAY, '23:00:00')),
  (4,  7, 8, 4, '노이즈캔슬링 성능이 기대 이상이에요.',                 TIMESTAMP(@today - INTERVAL 22 DAY, '07:40:00')),
  (5, 10, 8, 3, '케이스가 생각보다 커요. 소리는 괜찮습니다.',           TIMESTAMP(@today - INTERVAL 20 DAY, '12:10:00'));
