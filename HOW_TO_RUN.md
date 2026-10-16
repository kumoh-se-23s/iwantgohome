# 크라우드펀딩 시스템 - 11/2 DB 파트 검사 실행 안내

검사 항목 10개를 MVC 구조(Service → MyBatis Mapper → MySQL)로 구현한 코드입니다.
수업 방식대로 **Gradle + Lombok + DBCP 커넥션 풀(commons-dbcp2)** 을 사용합니다.
MySQL 8.0.46 에 `sql/crowdfunding.sql` 을 그대로 실행한 DB 에서 Gradle 로 `initDB → test01 ~ test10` 을 검사 순서대로 돌려 확인했습니다.

---

## 1. 처음 한 번만 하는 준비

| 순서 | 할 일 |
|---|---|
| 1 | MySQL Workbench 에서 `sql/crowdfunding.sql` 실행 (이미 했다면 생략). MySQL 은 **8.0.16 이상**이어야 CHECK 제약이 동작한다 (`SELECT VERSION();`) |
| 2 | IntelliJ → File → Open → 압축을 푼 **`crowdfunding` 폴더**(또는 그 안의 `build.gradle`) 선택 → *Trust Project* → 오른쪽 아래 Gradle 동기화가 끝날 때까지 기다린다 (MySQL 드라이버, Lombok, DBCP, MyBatis 를 자동으로 받음) |
| 3 | File → Project Structure → SDK 를 **JDK 17 또는 21** 로 |
| 4 | **Lombok 설정** (수업 자료와 동일) : Settings → Plugins 에서 *Lombok* 설치 확인 → Settings → Build, Execution, Deployment → Compiler → Annotation Processors → **Enable annotation processing** 체크 (안 되면 IntelliJ 재시작) |
| 5 | `src/main/resources/config/db.properties` 의 `DB_PWD` 를 **내 MySQL 비밀번호**로 수정 (아이디가 root 가 아니면 `DB_USER` 도) |

> 실행은 `Main.java` 의 초록색 ▶ 버튼. 콘솔 한글이 깨지면 8번 "자주 나는 오류" 참고.

## 2. 검사 순서 (과제 안내 방식 그대로)

`src/main/java/crowdfunding/Main.java` 에서 **한 줄만 주석을 풀고 실행 → 다시 주석**을 반복한다.

```
initDB 실행 → 주석 → test01 실행 → 주석 → test02 실행 → 주석 → ... → test10
```

| 순서 | 주석 해제할 줄 | 기대 결과 |
|---|---|---|
| 0 | `DataInitializer.initDB();` | 사용자 11, 프로젝트 8, 리워드 16, 후원 16, 좋아요 12, 리뷰 5 |
| 1 | `makerTest.test01ViewFundingStatus();` | maker01 의 프로젝트 1·2·3 : 총 펀딩 금액, 리워드별 신청/잔여 수량, 서포터와 배송지 |
| 2 | `makerTest.test02RegisterProject();` | 프로젝트 **9번** 생성, `approval_status = 'draft'` |
| 3 | `makerTest.test03RegisterReward();` | 리워드 없이 신청 → 실패 / 리워드 17·18번 등록 → 신청 → `'waiting'` |
| 4 | `adminTest.test04ApproveProject();` | 9번 → `'approved'` + `project_status = 'in_progress'` |
| 5 | `supporterTest.test05UpdateProfile();` | sup08(user 11) 의 연락처·비밀번호·배송지 변경 (user, supporter 두 테이블) |
| 6 | `supporterTest.test06ViewProjects();` | 승인된 프로젝트 7개 (승인 대기 6번, 거절 7번은 안 보임) |
| 7 | `supporterTest.test07FilterProjects();` | 테크·가전 + 펀딩중 → 9, 1, 5번 |
| 8 | `supporterTest.test08ApplyReward();` | 리워드 8번 신청 1→2개(잔여 9→8), 후원 기록 생성, 메이커 화면에 신청자 표시, 같은 프로젝트 재신청 → 실패 |
| 9 | `supporterTest.test09RegisterReview();` | 마감·성공 2번에 리뷰 등록(리워드명/별점/리뷰 출력), 마감·실패 3번 → 실패 |
| 10 | `supporterTest.test10ConcurrencyControl();` | 잔여 1개: sup05 성공(기록 O, 수량 3/3) → sup06 실패(기록 X, 수량 그대로) |
| 선택 | `supporterTest.test10bConcurrencyWithThreads();` | 잔여 2개에 6명이 **동시에** 신청 → 정확히 2명 성공 (누가 성공할지는 매번 다름) |

- 각 테스트는 실행 후 **Workbench 에서 돌릴 확인 SQL** 을 콘솔 끝에 출력한다. 검사 순서대로 모아 둔 파일: `sql/check_queries.sql`
- `initDB()` 는 모든 테이블을 비우고 다시 넣는다 → 리허설을 몇 번 해도 initDB 한 번이면 처음 상태, PK 도 항상 같다.
- test05 를 한 번 실행하면 sup08 의 비밀번호가 바뀌어서 두 번째 실행은 "현재 비밀번호 불일치"로 실패한다 → initDB 후 다시.
- **리허설용 한 번에 실행** (검사 당일에는 주석 방식으로)
  - IntelliJ: Run → Edit Configurations → Program arguments 에 `initDB test01 test02 test03 test04 test05 test06 test07 test08 test09 test10`
  - 터미널: `.\gradlew.bat run --args="initDB test01 test02 test03"` (맥/리눅스는 `./gradlew run --args="..."`)

## 3. 검사 때 바꿔도 되는 값 ("최소한의 코드 수정 허용")

각 테스트 클래스 **맨 위 상수**만 고치면 된다.

| 파일 | 상수 | 기본값 | 의미 |
|---|---|---|---|
| `MakerTest` | `MAKER_ID` | 2 | 검사하는 메이커 (2 maker01 / 3 maker02) |
| `MakerTest` | `TEST03_TARGET_PROJECT_ID` | 0 | 리워드 등록할 프로젝트. 0 = 가장 최근 '작성 중' 프로젝트 자동 |
| `AdminTest` | `TEST04_TARGET_PROJECT_ID` | 0 | 승인할 프로젝트. 0 = 가장 최근 '승인 대기' 자동 (initDB 직후면 6번) |
| `SupporterTest` | `TEST07_CATEGORY_ID` / `TEST07_STATUS` / `TEST07_KEYWORD` | 1 / `"IN_PROGRESS"` / null | 필터 조건 (`"CLOSED"` = 펀딩종료, null = 전체) |
| `SupporterTest` | `TEST08_*` | 4 / 4 / 8 | 신청할 서포터 / 프로젝트 / 리워드 |
| `SupporterTest` | `TEST09_*` | 4 / 2 / 5점 | 리뷰 쓸 서포터 / 프로젝트 / 별점 |
| `SupporterTest` | `TEST10_*` | 1 / 1 / 8 / 9 | 프로젝트 / 잔여 1개 리워드 / 성공할 서포터 / 실패할 서포터 |

## 4. 사전 데이터 (initDB 기준 번호)

| 계정 | 비밀번호 | user_id |
|---|---|---|
| admin01 (관리자) | admin1234 | 1 |
| maker01 김민수 | maker1234 | 2 (프로젝트 1·2·3·7) |
| maker02 이수진 | maker1234 | 3 (프로젝트 4·5·6·8) |
| sup01 ~ sup08 (서포터) | pass1234 | 4 ~ 11 |

| 번호 | 프로젝트 | 메이커 | 카테고리 | 상태 | 검사 용도 |
|---|---|---|---|---|---|
| 1 | 접이식 블루투스 키보드 | maker01 | 테크·가전 | 펀딩중 | 리워드 1번 한정 3개 중 2개 신청 → **test10** |
| 2 | 수제 그래놀라 정기배송 | maker01 | 푸드 | 마감·성공 | sup01 후원 → **test09 리뷰** |
| 3 | 휴대용 미니 빔프로젝터 | maker01 | 테크·가전 | 마감·실패 | test09 실패 확인 |
| 4 | 제주 감귤 착즙 주스 | maker02 | 푸드 | 펀딩중 | **test08 리워드 신청** (리워드 8번 한정 10개) |
| 5 | 스마트 식물 재배기 | maker02 | 테크·가전 | 펀딩중 (마감 임박) | test10b (리워드 9번 한정 2개) |
| 6 | 비건 단백질 쿠키 | maker02 | 푸드 | 승인 대기 | 관리자 승인 대상 (예비) |
| 7 | 전통 발효 식초 | maker01 | 푸드 | 승인 거절 | 2차 '수정 후 재신청' |
| 8 | 노이즈캔슬링 수면 이어폰 | maker02 | 테크·가전 | 마감·성공 | 리뷰 3개 |

과제 조건: 전체 프로젝트 5개 이상(8개), 한 메이커(maker01)의 후원 기록 있는 프로젝트 2개 이상(3개), 카테고리 2개, 프로젝트마다 리워드 2개 이상 + 수량 제한 리워드 포함, 이미 목표를 달성한 프로젝트(2·8번).
날짜는 **initDB 를 실행한 날 기준**으로 계산되므로 언제 실행해도 상태가 맞는다.

## 5. 패키지 구조 (MVC)

```
crowdfunding/                         ← IntelliJ 로 여는 폴더
├── build.gradle, settings.gradle     Gradle 설정 (수업 build.gradle 의존성 그대로 + application 플러그인)
├── gradlew, gradlew.bat, gradle/     Gradle Wrapper (Gradle 을 따로 설치하지 않아도 됨)
├── sql/                              crowdfunding.sql (스키마), check_queries.sql (검사 때 확인 쿼리)
└── src/main/
    ├── java/crowdfunding
    │   ├── Main.java                       검사 실행 + 객체 조립 (initDB + test01~10)
    │   ├── config/   PooledDataSource, MyBatisConfig, AppConfig   [공통]
    │   ├── exception/ BusinessException, DbErrors                  [공통]
    │   ├── model/
    │   │   ├── domain/  User, Supporter, Category, Project, Reward, Funding, Review   (테이블 1행 = 클래스 1개)
    │   │   ├── dto/     FundingStatus·RewardStatus·FunderInfo [D], ProjectSummary·ProjectSearchCondition [B],
    │   │   │            MyInfoForm [D], ReviewView [C]
    │   │   ├── enums/   Role, ApprovalStatus, ProjectStatus  (DB ENUM 값 ↔ 한글 이름)
    │   │   ├── dao/     *Mapper 인터페이스 (SQL 은 resources/mapper/*.xml)
    │   │   └── service/ MakerProjectService·AdminService [A], ProjectSearchService [B],
    │   │                FundingService·ReviewService [C], FundingStatusService·MyPageService [D],
    │   │                ProjectCloseService·ServiceGuard [공통]
    │   ├── view/     ResultView (콘솔 출력)
    │   └── dbtest/   DataInitializer [B], MakerTest, AdminTest, SupporterTest
    └── resources/
        ├── config/db.properties   DB 접속 정보 (3주차 강의와 같은 위치·키 이름)
        ├── mybatis-config.xml     MyBatis 설정 + Mapper 목록
        ├── mapper/*.xml           (ProjectMapper.xml 은 담당 구역별로 나눠 둠)
        └── sql/seed.sql           (initDB 가 실행하는 사전 데이터)
```

**담당(제안)** A = 검사 02·03·04 / B = 06·07 + initDB / C = 08·09·10 / D = 01·05.
테스트는 Mapper 를 직접 부르지 않고 **Service 를 호출**한다 → 12월 최종 시스템에서도 같은 Service 를 그대로 쓰고, 소켓·Controller·메뉴만 위에 얹으면 된다.

## 6. Gradle · Lombok · DBCP 를 쓰는 방식

**DB 연결 흐름** (3주차 강의의 PooledDataSource 를 그대로 쓰고, 그 위에 MyBatis 를 얹음)

```
config/db.properties → PooledDataSource (DBCP BasicDataSource, 커넥션 최대 10개)
                     → MyBatisConfig.createSqlSessionFactory(dataSource)
                     → SqlSessionFactory 를 각 Service 생성자에 넣어 줌 (Main 에서 조립)
```

`Main.java` 에서 강의 예제처럼 객체를 직접 만들어 조립한다.

```java
DataSource dataSource = PooledDataSource.getDataSource();
SqlSessionFactory sqlSessionFactory = MyBatisConfig.createSqlSessionFactory(dataSource);
FundingService fundingService = new FundingService(sqlSessionFactory);
...
SupporterTest supporterTest = new SupporterTest(myPageService, projectSearchService, ...);
```

**Lombok 을 쓴 곳**

| 위치 | 어노테이션 | 하는 일 |
|---|---|---|
| `domain/*`, `dto/*` | `@Getter @Setter @ToString` | getter / setter / toString 자동 생성 (수업 BoardDTO 와 같은 방식). 비밀번호 필드는 `@ToString.Exclude` 로 출력에서 뺌 |
| `dto/MyInfoForm` | `@NoArgsConstructor @AllArgsConstructor` | 빈 생성자 + 전체 필드 생성자 (필드 순서대로) |
| `dto/ProjectSearchCondition` | `@NoArgsConstructor` | `new ProjectSearchCondition()` = 조건 없는 검색 |
| `service/*`, `dbtest/*Test` | `@RequiredArgsConstructor` | `final` 필드를 받는 생성자 자동 생성 (수업 BoardDAO 와 같은 방식) |
| `enums/*` | `@Getter @RequiredArgsConstructor` | `getValue()`(DB 값), `getLabel()`(한글 이름) |

계산해서 돌려주는 메소드(`Reward.getRemaining()`, `FundingStatus.getTotalAmount()`, `ReviewView.getStars()` 등)는 직접 작성한 그대로 둔다.

**새 Service 를 추가할 때 (팀 규칙)**: 클래스에 `@RequiredArgsConstructor` 를 붙이고 `private final SqlSessionFactory sqlSessionFactory;` 필드를 둔다 → `Main` 에서 `new XxxService(sqlSessionFactory)` 로 만들어 필요한 곳에 넘긴다.

## 7. 동시성 제어 설명 (검사 10 시연용)

`FundingService.applyReward()` 전체가 **하나의 트랜잭션**이다.

1. **`SELECT ... FOR UPDATE`** 로 리워드 행을 잠근다 → 같은 리워드를 동시에 신청한 다른 트랜잭션은 여기서 기다렸다가, 앞사람이 끝난 뒤 **줄어든 수량**을 읽는다.
2. 잔여 수량이 없으면 예외 → **롤백** → 후원 기록도, 수량 변경도 남지 않는다.
3. **조건부 UPDATE** (`applied_count < reward_amount` 일 때만 +1) → 0행이면 실패 (이중 안전장치).
4. **funding INSERT** → PK `(user_id, project_id)` 가 같은 서포터의 중복 후원을 막는다.
5. **commit** 해야 3·4가 함께 반영된다. 최종 안전장치: 테이블 CHECK `applied_count ≤ reward_amount`.

검증 (DBCP 커넥션 10개 기준): 잔여 5개에 40명 동시 신청 → 정확히 5명 성공 / 같은 서포터가 10번 동시에 신청 → 1건만 성공 / 모든 리워드에서 `applied_count` = 실제 후원 행 수.

## 8. 코드에서 정한 규칙 (팀에서 한 번 확인)

| 규칙 | 위치 |
|---|---|
| 승인 신청에는 리워드 **2개 이상** + 수량 제한 리워드 1개 이상 (구현 범위 2.1, 2.2) | `AppConfig.MIN_REWARDS_TO_SUBMIT`, `REQUIRE_LIMITED_REWARD` |
| 리워드는 작성 중 / 거절 상태에서만 추가 (승인 뒤 가격이 바뀌면 후원 금액과 어긋나므로) | `MakerProjectService` |
| 후원 금액 = 신청한 리워드의 가격 (funding 에 금액 컬럼 없음) | `ProjectMapper.xml` |
| 펀딩 시작일 ≥ 오늘, 종료일 ≥ 시작일, 리워드 제공 시기 ≥ 펀딩 종료일 | `MakerProjectService` |
| 종료일이 지난 진행 중 프로젝트 → 총액 ≥ 목표면 success, 아니면 fail (조회·리뷰 전에 자동 처리) | `ProjectCloseService` |
| 리뷰: 본인이 후원 + 마감 + 펀딩 성공 + 1회, 별점 1~5 | `ReviewService` |
| 비밀번호 변경 시 현재 비밀번호 확인 | `MyPageService` |
| 날짜 판단은 자바의 한국 날짜 기준 (`AppConfig.today()`) | `AppConfig` |

## 9. 자주 나는 오류

| 메시지 | 해결 |
|---|---|
| `DB_PWD 를 내 MySQL 비밀번호로 바꿔 주세요` | `src/main/resources/config/db.properties` 수정 |
| `Access denied for user` | `config/db.properties` 의 `DB_USER` / `DB_PWD` 확인 |
| `Unknown database 'crowdfunding'` / `Table ... doesn't exist` | Workbench 에서 `sql/crowdfunding.sql` 먼저 실행 |
| `Communications link failure` | MySQL 서버가 켜져 있는지, 포트 3306 확인 |
| 펀딩 현황/목록이 비어 있음, FK 오류 | `initDB()` 를 먼저 실행 |
| `cannot find symbol ... getXxx()` / `setXxx()` (빨간 줄) | Lombok 설정 안 됨 → 1번 표의 4번 (플러그인 + Enable annotation processing) 후 IntelliJ 재시작 |
| Lombok 관련 `ExceptionInInitializerError` / `TypeTag :: UNKNOWN` | JDK 가 너무 최신(24 이상)이라 Lombok 1.18.36 이 지원하지 않음 → SDK 를 17 또는 21 로 (또는 `build.gradle` 의 lombok 두 줄을 1.18.40 이상으로) |
| `Unsupported class file major version` (Gradle 동기화 실패) | Settings → Build Tools → Gradle → **Gradle JVM** 을 17 또는 21 로 |
| 의존성 다운로드 실패 | 인터넷 연결 확인 후 오른쪽 Gradle 탭의 🔄 (Reload All Gradle Projects) |
| 콘솔 한글 깨짐 | Settings → Build Tools → Gradle → *Build and run using* 을 **IntelliJ IDEA** 로 바꾸고, Run Configuration 의 VM options 에 `-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8` |
| MyBatis 가 실행하는 SQL 을 보고 싶다 | `mybatis-config.xml` 의 `STDOUT_LOGGING` 줄 주석 해제 |
