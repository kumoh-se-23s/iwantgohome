# 크라우드펀딩 시스템 — Java · MVC · MyBatis · TCP/IP

| 단계 | 일정 | 진입점 | 상태 |
|---|---|---|---|
| 1차 DB 파트 검사 | 11/02 | `Main` (initDB + test01~10) | 완성 — MySQL 8.0.46 에서 전 항목 실행 확인 |
| 2차 구현 데모 | 12/07 | `ServerMain` + `ClientMain` | 서버·클라이언트·로그인·역할별 메뉴 동작, 1차 기능 메뉴 연결 완료. 2차 전용 기능은 아래 로드맵 위치에 추가 |

**핵심 설계: 1차 검사 코드와 2차 서버가 같은 Controller → Service → MyBatis Mapper 를 호출한다.**
2차에서는 기존 코드를 고치지 않고 소켓 통신과 메뉴(View)만 얹는다.

```
1차 검사   Main → dbtest.*Test ─────────────────────────────────┐
                                                                  ▼
2차 데모   ClientMain → view.*Menu → ServerConnection ══TCP══▶ ClientHandler → RequestDispatcher
                                                                  │
                                         controller (권한 검사, 예외 → Response)
                                                                  │
                                         model.service (트랜잭션, 업무 규칙, ★동시성 제어)
                                                                  │
                                         model.dao (MyBatis Mapper + XML, ★동적 쿼리)
                                                                  │
                                                               MySQL
```

---

## 1. 실행 준비

1. **DB 만들기** — MySQL Workbench 에서 `sql/crowdfunding.sql` 실행 (처음 한 번)
2. **접속 정보** — `src/main/resources/db.properties` 의 `db.username`, `db.password` 를 내 MySQL 계정으로 수정
   (파일을 고치지 않고 실행 옵션 `-Ddb.password=xxxx` 로 덮어쓸 수도 있음)
3. **IntelliJ** — File → Open → `pom.xml` 선택 → *Open as Project*
   Maven 이 MyBatis 3.5.17, MySQL Connector/J 8.4.0 을 자동으로 받는다. JDK 17 이상.
4. 콘솔 한글이 깨지면: Settings → Editor → File Encodings 를 UTF-8 로, 또는 VM 옵션 `-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8`

## 2. 1차 검사 실행 순서 (과제 안내 방식 그대로)

`Main.java` 에서 **한 줄만 주석을 풀고 실행 → 다시 주석** 을 반복한다.

| 순서 | 주석 해제할 줄 | 결과 (Workbench 에서 확인) |
|---|---|---|
| 0 | `initDB();` | 11개 사용자, 프로젝트 8개, 리워드 16개, 후원 15건, 좋아요 12건, 리뷰 4건 |
| 1 | `makerTest.test01ViewFundingStatus();` | maker01 의 프로젝트별 총 펀딩 금액, 리워드별 신청/잔여 수량, 서포터·배송지 출력 |
| 2 | `makerTest.test02RegisterProject();` | project 9번 생성, `approval_status = 'draft'` |
| 3 | `makerTest.test03RegisterReward();` | 리워드 없이 신청 → 실패, 리워드 17·18번 등록 → 신청 → `'waiting'` |
| 4 | `adminTest.test04ApproveProject();` | 9번 → `'approved'` + `project_status = 'in_progress'` |
| 5 | `supporterTest.test05UpdateProfile();` | sup08 의 이름·연락처·배송지·비밀번호 변경 (user, supporter 두 테이블) |
| 6 | `supporterTest.test06ViewProjects();` | 승인된 프로젝트 전체 (최신순) |
| 7 | `supporterTest.test07FilterProjects();` | 테크·가전+펀딩중 / 푸드+펀딩종료 필터 (MyBatis 동적 쿼리) |
| 8 | `supporterTest.test08ApplyReward();` | sup01 이 4번 프로젝트 7번 리워드 신청 → 성공, 같은 프로젝트 재신청 → 실패 |
| 9 | `supporterTest.test09RegisterReview();` | sup01 이 2번(펀딩 성공) 프로젝트에 리뷰 → 성공, 진행 중 1번 → 실패 |
| 10 | `supporterTest.test10ConcurrencyControl();` | 잔여 1개: sup04 성공(기록 생성, applied_count 3) / sup05 실패(기록 없음, 수량 그대로) |
| (선택) | `supporterTest.test10bConcurrencyWithThreads();` | 잔여 2개에 6명 동시 신청 → 정확히 2명 성공 |

- 각 테스트는 실행 후 **Workbench 에서 돌려볼 확인 SQL** 을 콘솔에 함께 출력한다. 모아 둔 파일: `sql/check_queries.sql`
- 검사 때 값을 바꿔야 하면(대상 PK, 필터 조건) 각 테스트 클래스 맨 위의 `TEST0x_...` 상수만 고치면 된다.
- `initDB()` 는 모든 테이블을 TRUNCATE 하고 다시 넣으므로 **몇 번을 실행해도 PK 가 항상 같다**.
- 주석 대신 실행 인자로도 가능: `java crowdfunding.Main initDB test01 test02 ...`

## 3. 사전 데이터 (initDB)

날짜는 **initDB 를 실행한 날 기준**으로 만들어진다 → 언제 실행해도 진행 중 / 종료(성공·실패) 프로젝트가 생긴다.

| 계정 | 비밀번호 | 비고 |
|---|---|---|
| admin01 | admin1234 | 관리자 |
| maker01 (김민수, user_id 2) | maker1234 | 검사용 메이커 — 프로젝트 1·2·3·7 |
| maker02 (이수진, user_id 3) | maker1234 | 프로젝트 4·5·6·8 |
| sup01 ~ sup08 (user_id 4~11) | pass1234 | 서포터 (test05 후 sup08 은 newpass5678) |

| 번호 | 프로젝트 | 메이커 | 카테고리 | 상태 | 검사 용도 |
|---|---|---|---|---|---|
| 1 | 접이식 블루투스 키보드 | maker01 | 테크·가전 | 펀딩중 | 리워드 1번 한정 3개 중 2개 신청 → **test10** |
| 2 | 수제 그래놀라 정기배송 | maker01 | 푸드 | 종료·성공 | sup01 후원 → **test09 리뷰** |
| 3 | 휴대용 미니 빔프로젝터 | maker01 | 테크·가전 | 종료·실패 | 실패 처리 확인 |
| 4 | 제주 감귤 착즙 주스 | maker02 | 푸드 | 펀딩중 | **test08 리워드 신청** |
| 5 | 스마트 식물 재배기 | maker02 | 테크·가전 | 펀딩중(마감임박) | 리워드 9번 한정 2개 → test10b |
| 6 | 비건 단백질 쿠키 | maker02 | 푸드 | 승인 대기 | 관리자 승인 대상 |
| 7 | 전통 발효 식초 | maker01 | 푸드 | 승인 거절 | 2차 '수정 후 재신청' 시연 |
| 8 | 노이즈캔슬링 수면 이어폰 | maker02 | 테크·가전 | 종료·성공 | 리뷰 3개 |

과제 조건 충족: 프로젝트 6개 이상(8개), 카테고리 2개, 프로젝트마다 리워드 2개 이상 + 한정 수량 리워드 포함,
한 메이커(maker01)가 후원 기록이 있는 프로젝트 2개 이상(3개), 테스트 당일 이미 목표를 달성한 프로젝트 존재(2·8번).

## 4. 패키지 구조 (MVC)

```
crowdfunding
├── Main.java                 1차 DB 검사 진입점 (initDB + test01~10)
├── ServerMain.java           2차 서버 실행
├── ClientMain.java           2차 클라이언트 실행
│
├── model/                    ── M ─────────────────────────────────────
│   ├── domain/               테이블 1행 = 클래스 1개 (User, Project, Reward, Funding ...)
│   ├── dto/                  화면/조회용 묶음 (ProjectSummary, FundingStatus, ProjectSearchCondition ...)
│   ├── enums/                DB ENUM ↔ 자바 enum (Role, ApprovalStatus, ProjectStatus ...)
│   ├── dao/                  MyBatis Mapper 인터페이스 (SQL 은 resources/mapper/*.xml)
│   └── service/              업무 규칙 + 트랜잭션 + 동시성 제어 (MakerService, SupporterService ...)
│
├── controller/               ── C ─────────────────────────────────────
│   ├── *Controller.java      권한 확인 → 서비스 호출 → Response 로 결과 반환
│   └── RequestDispatcher     (2차) 네트워크 Command → 컨트롤러 메소드 연결
│
├── view/                     ── V ─────────────────────────────────────
│   ├── ResultView            결과 출력 (1차 검사와 2차 클라이언트가 같이 사용)
│   ├── ConsoleInput          입력 + 재입력 처리
│   └── ClientApp, *Menu      (2차) 로그인 → 역할별 메뉴 → 로그아웃
│
├── protocol/                 Command, Request, Response — 클라이언트·서버 공통 메시지 (Serializable)
├── network/server/           FundingServer(멀티스레드), ClientHandler(접속당 1스레드), ClientSession
├── network/client/           ServerConnection
├── config/                   MyBatisConfig, TransactionManager, AppContext(객체 조립), AppConfig
└── dbtest/                   1차 검사 전용: DataInitializer(initDB), MakerTest, AdminTest, SupporterTest
```

## 5. 동시성 제어 방법 (시연 설명용)

`SupporterService.applyReward()` — 리워드 신청 전체가 **하나의 트랜잭션**이다.

1. **비관적 락** — `SELECT ... FROM reward WHERE reward_id = ? FOR UPDATE`
   같은 리워드를 동시에 신청한 다른 트랜잭션은 이 줄에서 대기한다. 먼저 락을 잡은 요청이 commit 하면 다음 요청이 **줄어든 수량**을 읽는다.
2. **잔여 수량 확인** — 없으면 예외 → 롤백 → 후원 기록도, 수량 변경도 남지 않는다.
3. **조건부 UPDATE** — `applied_count = applied_count + 1 WHERE ... applied_count < reward_amount`
4. **funding INSERT** — PK `(user_id, project_id)` 때문에 같은 서포터의 중복 후원은 DB 가 거절 → 롤백
5. **최종 안전장치** — 테이블 CHECK `ck_reward_applied (applied_count <= reward_amount)`

검증 결과: 잔여 5개에 40명 동시 신청 → 정확히 5명 성공 / 같은 서포터가 10번 동시 신청 → 1건만 성공 / 모든 리워드에서 `applied_count = 실제 후원 행 수`.

## 6. 기존과 바뀐 부분

### DB 스키마 (`crowdfunding.sql`) — 변경 없음
원본 파일을 그대로 `sql/` 에 넣었고 한 글자도 바꾸지 않았다. 프로그램이 스키마에 맞춰 동작한다.

### 스키마에는 없고 코드에서 정한 규칙 (팀 확인 필요)
| 규칙 | 이유 |
|---|---|
| 후원 금액 = 신청한 리워드의 `reward_price` (funding 에 금액 컬럼 없음) | 그래서 리워드는 작성 중/거절 상태에서만 추가 가능 (승인 후 가격 변경 불가) |
| 승인 신청에는 리워드 1개 이상 필요 (`AppConfig.MIN_REWARDS_TO_SUBMIT`) | "리워드 등록 후 프로젝트 신청" |
| 거절된 프로젝트 재신청 시 `reject_reason` 을 NULL 로 지움 | 승인 대기 상태에 옛 사유가 남지 않도록. 이력이 필요하면 지우지 않게 바꿔도 CHECK 위반 아님 |
| 서포터 관점 '펀딩중' = `in_progress` 이고 종료일이 오늘 이후 | 마감 처리 전이라도 종료일이 지나면 '펀딩종료'로 보임 |
| 종료일이 지난 진행 중 프로젝트 → 총액 ≥ 목표면 success, 아니면 fail | initDB 마지막 + 2차 서버 시작 시 + 1분마다 자동 처리 |
| 날짜 판단은 자바 `AppConfig.today()` (Asia/Seoul) 기준 | DB `CURDATE()` 와 시간대가 어긋나는 문제 방지 |
| 시작일 ≥ 오늘, 종료일 ≥ 시작일, 리워드 제공 시기 ≥ 펀딩 종료일 | 입력 검증 |
| 리뷰: 펀딩 **성공** + 본인이 후원 + 1회 | 요구사항 4.5 (성공 = 배송 완료로 간주) |
| 비밀번호 변경 시 현재 비밀번호 확인 | MyPage 보안 |

### 1차 → 2차 문서에서 달라진 점
- 검사 항목 번호가 다르다 (1차 1번 '펀딩 현황' = 2차 4번 등). 기능 이름 기준으로 매칭해 두었다.
- 2차에 새로 생긴 항목: 거절 시 수정·재신청, 리뷰 조회(최신순), 거절·사유, 관리자 펀딩 현황 + 정렬, 서포터 정렬·상세 보기, 리워드 취소, 좋아요, MyPage 후원 내역, 로그인, 회원가입
- "동적 쿼리는 반드시 MyBatis" (문서에는 '숙소 검색'으로 적혀 있지만 이 주제에서는 프로젝트 검색) → **1차부터 MyBatis 로 작성**해서 2차에 다시 짤 필요 없음
- "카테고리는 2개로 제한" → 사전 데이터 카테고리 2개
- "기능마다 프로그램을 재시작하지 않는 메뉴" → 2차 클라이언트는 기능 실행 후 같은 메뉴로 돌아옴

## 7. 2차 로드맵 (최종 20개 항목)

| # | 기능 | 상태 | 2차에서 할 일 |
|---|---|---|---|
| 1 | 프로젝트 기본 정보 등록 | ✅ | — |
| 2 | 리워드 등록 후 신청 | ✅ | — |
| 3 | (거절 시) 수정 및 재신청 | 재신청 ✅ / 수정 ➕ | `MakerService.updateProject` + `ProjectMapper.updateProjectInfo` |
| 4 | 프로젝트별 펀딩 현황 | ✅ | — |
| 5 | 프로젝트 리뷰 조회(최신순) | 쿼리 ✅ | 메이커 메뉴에 연결 (`GET_PROJECT_REVIEWS` 재사용) |
| 6 | 관리자 승인 | ✅ | — |
| 7 | 거절 및 사유 작성 | ➕ | `AdminService.rejectProject` + `ProjectMapper.reject` |
| 8 | 프로젝트별 펀딩 현황(최신순) | 상세 ✅ | 목록 쿼리 + `MakerService.buildFundingStatus` 재사용 |
| 9 | 정렬(총액/달성률/서포터 수, 오름·내림) | ➕ | `ProjectMapper.xml` 에 `<choose>` ORDER BY |
| 10 | 개인정보 수정 | ✅ | — |
| 11 | 프로젝트 보기 | ✅ | — |
| 12 | 필터링(카테고리, 진행/마감) | ✅ | — |
| 13 | 정렬(모집금액·마감임박·좋아요) | ✅ | (동적 쿼리에 이미 포함) |
| 14 | 상세 보기 / 리워드 신청 | ✅ | — |
| 15 | 리워드 취소 | ➕ | `SupporterService.cancelFunding` (FOR UPDATE → applied_count - 1 → DELETE) |
| 16 | 좋아요/취소 토글 | ➕ | `SupporterService.toggleLike` + `LikeMapper.deleteLike` |
| 17 | MyPage 후원 내역 | ➕ | `FundingMapper.findMyFundings` (리뷰 작성 여부 LEFT JOIN) |
| 18 | 리뷰·별점 | ✅ | — |
| 19 | 동시성 제어 | ✅ | — |
| 20 | 로그인 | ✅ | (+ 회원가입 ➕ `AuthService.signUp`) |

**기능 하나 추가하는 순서** (기존 코드는 건드리지 않고 추가만):
`Mapper.xml + 인터페이스` → `Service 메소드` → `Controller 메소드` → `Command` 상수 → `RequestDispatcher` case 한 줄 → `*Menu` 메뉴 항목.
각 파일에 `[2차]` 주석으로 위치를 표시해 두었다.

## 8. 2차 실행 (서버 1 + 클라이언트 2)

```
# 서버 노트북 (MySQL 있음)
java -cp target/crowdfunding.jar crowdfunding.ServerMain            # 포트 9000
java -cp target/crowdfunding.jar crowdfunding.ClientMain            # 같은 노트북에서 admin01 로그인

# 메이커 / 서포터 노트북 (DB 설정 불필요)
java -cp target/crowdfunding.jar crowdfunding.ClientMain 192.168.0.10 9000
```
`mvn package` 로 `target/crowdfunding.jar` (의존성 포함)를 만든다. IntelliJ 에서는 각 main 을 바로 실행해도 된다.
통신은 `ObjectOutputStream` 으로 `Request`/`Response` 객체를 주고받는다. 수업에서 정한 바이트 프로토콜이 있다면
`ClientHandler` 와 `ServerConnection` 두 클래스만 바꾸면 된다.

## 9. 자주 나는 오류

| 메시지 | 해결 |
|---|---|
| `Access denied for user` | `db.properties` 계정/비밀번호 확인 |
| `Unknown database 'crowdfunding'` / `Table ... doesn't exist` | `sql/crowdfunding.sql` 먼저 실행 |
| `Public Key Retrieval is not allowed` | URL 에 `allowPublicKeyRetrieval=true` (이미 포함됨) |
| `로그인 실패 — initDB() 를 먼저 실행했는지 확인` | initDB 실행. test05 를 한 번 실행했다면 sup08 비밀번호가 바뀐 상태 |
| `Connection refused` (클라이언트) | 서버 먼저 실행, IP/포트·방화벽 확인 |
