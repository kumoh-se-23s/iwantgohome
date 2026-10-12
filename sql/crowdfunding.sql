-- =====================================================================
-- 크라우드펀딩 시스템 테이블 생성 스크립트 (MySQL 8.0.16 이상)
--   기준: ERDCloud ERD (2026-10-09 버전)
--   [보완] = ERD에 없거나 비어 있던 부분을 요구사항에 맞게 채운 곳
--   [수정] = ERD와 다르게 바꾼 곳
--   MySQL 8.0.46에서 실행 및 제약조건 동작 검증 완료
-- =====================================================================

CREATE DATABASE IF NOT EXISTS crowdfunding
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;
USE crowdfunding;

-- ---------------------------------------------------------------------
-- 사용자
-- ---------------------------------------------------------------------
CREATE TABLE `user` (
    user_id       INT          NOT NULL AUTO_INCREMENT COMMENT '유저 식별자',
    -- [보완] 길이 지정. [수정] 대소문자 구분(utf8mb4_bin): 기본 정렬이면 로그인 시 'ABC'와 'abc'가 같다고 판단됨
    id            VARCHAR(10)  CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '유저 아이디 (5~10자)',
    pw            VARCHAR(16)  CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '비밀번호 (8~16자)',
    role          ENUM('supporter','maker','admin') NOT NULL COMMENT '권한',
    user_name     VARCHAR(10)  NOT NULL COMMENT '이름 (2~10자)',      -- [수정] NVARCHAR → VARCHAR(utf8mb4)
    phone_number  VARCHAR(13)  NOT NULL COMMENT '연락처 010-0000-0000',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_user_login_id (id),
    -- [보완] ERD Domain에 적힌 규칙을 CHECK로 강제
    CONSTRAINT ck_user_id_len    CHECK (CHAR_LENGTH(id) BETWEEN 5 AND 10),
    CONSTRAINT ck_user_pw_len    CHECK (CHAR_LENGTH(pw) BETWEEN 8 AND 16),
    CONSTRAINT ck_user_name_len  CHECK (CHAR_LENGTH(user_name) BETWEEN 2 AND 10),
    CONSTRAINT ck_user_phone     CHECK (phone_number REGEXP '^010-[0-9]{4}-[0-9]{4}$')
) ENGINE=InnoDB COMMENT='사용자';

-- ---------------------------------------------------------------------
-- 서포터 (user와 1:0..1)
-- ---------------------------------------------------------------------
CREATE TABLE supporter (
    user_id           INT          NOT NULL COMMENT '유저 식별자',
    delivery_address  VARCHAR(255) NOT NULL COMMENT '배송지',
    birth_date        DATE         NULL     COMMENT '생년월일',
    PRIMARY KEY (user_id),
    CONSTRAINT fk_supporter_user FOREIGN KEY (user_id) REFERENCES `user` (user_id)
) ENGINE=InnoDB COMMENT='서포터';

-- ---------------------------------------------------------------------
-- 카테고리
-- ---------------------------------------------------------------------
CREATE TABLE category (
    category_id    INT          NOT NULL AUTO_INCREMENT COMMENT '카테고리 식별자',
    category_name  VARCHAR(255) NOT NULL COMMENT '카테고리 이름',
    PRIMARY KEY (category_id),
    UNIQUE KEY uk_category_name (category_name)
) ENGINE=InnoDB COMMENT='카테고리';

-- ---------------------------------------------------------------------
-- 프로젝트
-- ---------------------------------------------------------------------
CREATE TABLE project (
    project_id           INT           NOT NULL AUTO_INCREMENT COMMENT '프로젝트 식별자',
    project_name         VARCHAR(255)  NOT NULL COMMENT '프로젝트 이름',
    project_description  TEXT          NULL     COMMENT '프로젝트 설명',   -- [보완] 길이 미지정 → TEXT
    -- [수정] 'draft'(작성 중) 추가, 기본값 waiting → draft
    --        검사 항목 "리워드 등록 후 프로젝트 신청": 리워드 등록 전에는 승인 대기 목록에 보이면 안 됨
    approval_status      ENUM('draft','waiting','approved','reject') NOT NULL DEFAULT 'draft' COMMENT '승인 상태',
    reject_reason        VARCHAR(255)  NULL     COMMENT '거절 사유',
    project_status       ENUM('in_progress','success','fail') NULL COMMENT '프로젝트 상태 (승인 전 NULL)',
    start_date           DATE          NOT NULL COMMENT '시작 날짜',
    end_date             DATE          NOT NULL COMMENT '종료 날짜',
    target_amount        INT           NOT NULL COMMENT '목표 금액',
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '프로젝트 만든 시간',
    user_id              INT           NOT NULL COMMENT '메이커 유저 식별자',
    category_id          INT           NOT NULL COMMENT '카테고리 식별자',
    PRIMARY KEY (project_id),
    KEY ix_project_list (approval_status, project_status, end_date),   -- [보완] 서포터 목록/필터 조회용
    CONSTRAINT fk_project_user     FOREIGN KEY (user_id)     REFERENCES `user` (user_id),
    CONSTRAINT fk_project_category FOREIGN KEY (category_id) REFERENCES category (category_id),
    -- [보완] 요구사항 규칙을 DB에서도 보장
    CONSTRAINT ck_project_period   CHECK (start_date <= end_date),
    CONSTRAINT ck_project_target   CHECK (target_amount > 0),
    CONSTRAINT ck_project_status   CHECK ((approval_status = 'approved') = (project_status IS NOT NULL)),
    CONSTRAINT ck_project_reject   CHECK (approval_status <> 'reject' OR reject_reason IS NOT NULL)
) ENGINE=InnoDB COMMENT='프로젝트';

-- ---------------------------------------------------------------------
-- 리워드
-- ---------------------------------------------------------------------
CREATE TABLE reward (
    reward_id             INT           NOT NULL AUTO_INCREMENT COMMENT '리워드 식별자',
    reward_name           VARCHAR(255)  NOT NULL COMMENT '리워드 이름',
    reward_amount         INT           NULL     COMMENT '리워드 수량 (NULL = 무제한)',
    reward_price          INT           NOT NULL COMMENT '리워드 가격',
    applied_count         INT           NOT NULL DEFAULT 0 COMMENT '주문한 리워드 수량',
    reward_delivery_date  DATETIME      NULL     COMMENT '리워드 제공 시기',
    reward_description    VARCHAR(255)  NULL     COMMENT '리워드 제공 내용',
    project_id            INT           NOT NULL COMMENT '프로젝트 식별자',
    -- [보완] 이미지 원본 저장. BLOB은 최대 64KB라 사진 한 장도 부족 → MEDIUMBLOB(최대 16MB)
    reward_image          MEDIUMBLOB    NULL     COMMENT '이미지 원본 바이트',
    reward_image_type     VARCHAR(20)   NULL     COMMENT '이미지 형식 (image/png, image/jpeg 등)',
    PRIMARY KEY (reward_id),
    -- [보완] funding이 (리워드, 프로젝트)를 함께 참조하기 위한 키
    UNIQUE KEY uk_reward_project (reward_id, project_id),
    CONSTRAINT fk_reward_project FOREIGN KEY (project_id) REFERENCES project (project_id),
    CONSTRAINT ck_reward_price   CHECK (reward_price > 0),
    CONSTRAINT ck_reward_amount  CHECK (reward_amount IS NULL OR reward_amount >= 1),
    -- [보완] 이미지가 있으면 형식도 함께 저장, 크기는 5MB 이하 (소켓 전송 부담 제한)
    CONSTRAINT ck_reward_image   CHECK ((reward_image IS NULL) = (reward_image_type IS NULL)
                                        AND (reward_image IS NULL OR LENGTH(reward_image) <= 5242880)),
    -- [보완] 한정 수량 초과 신청을 DB에서 차단 (동시성 제어의 최종 안전장치)
    CONSTRAINT ck_reward_applied CHECK (applied_count >= 0
                                        AND (reward_amount IS NULL OR applied_count <= reward_amount))
) ENGINE=InnoDB COMMENT='리워드';

-- ---------------------------------------------------------------------
-- 신청 현황(후원): 서포터는 프로젝트당 1번만 후원 → PK (user_id, project_id)
-- ---------------------------------------------------------------------
CREATE TABLE funding (
    user_id      INT       NOT NULL COMMENT '유저 식별자',
    project_id   INT       NOT NULL COMMENT '프로젝트 식별자',
    funded_date  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '신청일',  -- [보완] 기본값
    reward_id    INT       NOT NULL COMMENT '리워드 식별자',
    PRIMARY KEY (user_id, project_id),
    KEY ix_funding_project (project_id, funded_date),
    -- [수정] user 대신 supporter 참조: 서포터만 후원 가능 + 메이커가 볼 배송지가 반드시 존재
    CONSTRAINT fk_funding_supporter FOREIGN KEY (user_id) REFERENCES supporter (user_id),
    -- [보완] 신청한 리워드가 반드시 같은 프로젝트의 리워드여야 함
    CONSTRAINT fk_funding_reward    FOREIGN KEY (reward_id, project_id) REFERENCES reward (reward_id, project_id)
) ENGINE=InnoDB COMMENT='신청 현황';

-- ---------------------------------------------------------------------
-- 좋아요: 서포터당 프로젝트 1개 → PK (project_id, user_id)
-- ---------------------------------------------------------------------
CREATE TABLE project_like (
    project_id  INT  NOT NULL COMMENT '프로젝트 식별자',
    user_id     INT  NOT NULL COMMENT '유저 식별자',
    PRIMARY KEY (project_id, user_id),
    KEY ix_like_user (user_id),
    CONSTRAINT fk_like_project   FOREIGN KEY (project_id) REFERENCES project (project_id),
    CONSTRAINT fk_like_supporter FOREIGN KEY (user_id)    REFERENCES supporter (user_id)   -- [수정] supporter 참조
) ENGINE=InnoDB COMMENT='좋아요';

-- ---------------------------------------------------------------------
-- 리뷰: 후원한 프로젝트에만, 1인 1리뷰
-- ---------------------------------------------------------------------
CREATE TABLE review (
    review_id       INT       NOT NULL AUTO_INCREMENT COMMENT '리뷰 식별자',
    review_content  TEXT      NULL     COMMENT '리뷰 내용',      -- [보완] 길이 미지정 → TEXT
    rating          TINYINT   NOT NULL COMMENT '별점 1~5',
    review_at       DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '리뷰 작성 시간',
    user_id         INT       NOT NULL COMMENT '유저 식별자',
    project_id      INT       NOT NULL COMMENT '프로젝트 식별자',
    PRIMARY KEY (review_id),
    UNIQUE KEY uk_review_once (user_id, project_id),             -- [보완] 프로젝트당 1리뷰
    KEY ix_review_project (project_id, review_at),               -- [보완] 프로젝트별 최신순 조회용
    -- ERD의 funding ↔ review 관계: 후원 기록이 있어야만 리뷰 가능
    CONSTRAINT fk_review_funding FOREIGN KEY (user_id, project_id) REFERENCES funding (user_id, project_id),
    CONSTRAINT ck_review_rating  CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB COMMENT='리뷰';