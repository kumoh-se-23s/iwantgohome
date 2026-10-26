package crowdfunding;

import crowdfunding.config.MyBatisConfig;
import crowdfunding.config.PooledDataSource;
import crowdfunding.dbtest.AdminTest;
import crowdfunding.dbtest.DataInitializer;
import crowdfunding.dbtest.MakerTest;
import crowdfunding.dbtest.SupporterTest;
import crowdfunding.model.service.AdminService;
import crowdfunding.model.service.FundingService;
import crowdfunding.model.service.FundingStatusService;
import crowdfunding.model.service.MakerProjectService;
import crowdfunding.model.service.MyPageService;
import crowdfunding.model.service.ProjectCloseService;
import crowdfunding.model.service.ProjectSearchService;
import crowdfunding.model.service.ReviewService;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

/**
 * 11/2 DB 파트 검사 실행 클래스
 * <p>
 * 검사 순서 (과제 안내 방식 그대로)
 * initDB 주석 해제 → 실행 → initDB 주석
 * → test01 주석 해제 → 실행 → test01 주석 → test02 주석 해제 → 실행 → ... → test10
 * <p>
 * 각 테스트는 실행 뒤 Workbench 에서 돌려 볼 "확인 SQL" 을 콘솔에 같이 출력한다.
 * 검사 때 대상 PK·필터 조건을 바꿔야 하면 각 테스트 클래스 맨 위의 상수만 고치면 된다.
 */
public class Main {

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(Main::explainError);   // DB 연결 오류 등이 나면 해결 방법 안내

        // ① 커넥션 풀(DBCP) → ② MyBatis  (3주차 강의처럼 Main 에서 객체를 만들어 조립한다)
        DataSource dataSource = PooledDataSource.getDataSource();
        SqlSessionFactory sqlSessionFactory = MyBatisConfig.createSqlSessionFactory(dataSource);

        // ③ Service : 생성자는 Lombok @RequiredArgsConstructor 가 만들어 준다 (final 필드 순서대로)
        ProjectCloseService projectCloseService = new ProjectCloseService(sqlSessionFactory);
        MakerProjectService makerProjectService = new MakerProjectService(sqlSessionFactory);
        AdminService adminService = new AdminService(sqlSessionFactory);
        FundingStatusService fundingStatusService = new FundingStatusService(sqlSessionFactory, projectCloseService);
        ProjectSearchService projectSearchService = new ProjectSearchService(sqlSessionFactory, projectCloseService);
        MyPageService myPageService = new MyPageService(sqlSessionFactory);
        FundingService fundingService = new FundingService(sqlSessionFactory);
        ReviewService reviewService = new ReviewService(sqlSessionFactory, projectCloseService);

        // ④ 검사 클래스
        MakerTest makerTest = new MakerTest(fundingStatusService, makerProjectService);
        AdminTest adminTest = new AdminTest(adminService);
        SupporterTest supporterTest = new SupporterTest(
                myPageService, projectSearchService, fundingService, fundingStatusService, reviewService);

        if (args.length > 0) {   // (리허설용) 실행 인자로 순서대로 실행: initDB test01 test02 ...
            runByArgs(args, makerTest, adminTest, supporterTest);
            return;
        }
        // 검사할 항목 하나만 주석 해제하여 실행
         DataInitializer.initDB();                       // 검사에 필요한 사전 데이터를 insert하는 메소드

//        makerTest.test01ViewFundingStatus();            // MAKER     - 프로젝트별 펀딩 현황 보기
//        makerTest.test02RegisterProject();              // MAKER     - 프로젝트 기본 정보 등록
//        makerTest.test03RegisterReward();               // MAKER     - 리워드 등록 및 프로젝트 신청
//        adminTest.test04ApproveProject();               // ADMIN     - 프로젝트 등록 승인
        supporterTest.test05UpdateProfile();            // SUPPORTER - 개인정보 수정
        supporterTest.test06ViewProjects();             // SUPPORTER - 프로젝트 보기
//        supporterTest.test07FilterProjects();           // SUPPORTER - 프로젝트 필터링
//        supporterTest.test08ApplyReward();              // SUPPORTER - 리워드 신청
//        supporterTest.test09RegisterReview();           // SUPPORTER - 리뷰 및 별점 등록
//        supporterTest.test10ConcurrencyControl();       // SUPPORTER - 제공 가능 수량 동시성 제어
    }
    /**
     * 실행 인자 예) initDB test01 test02 test03 test04 test05 test06 test07 test08 test09 test10
     */
    private static void runByArgs(String[] args, MakerTest maker, AdminTest admin, SupporterTest supporter) {
        for (String arg : args) {
            switch (arg) {
                case "initDB" -> DataInitializer.initDB();
                case "test01" -> maker.test01ViewFundingStatus();
                case "test02" -> maker.test02RegisterProject();
                case "test03" -> maker.test03RegisterReward();
                case "test04" -> admin.test04ApproveProject();
                case "test05" -> supporter.test05UpdateProfile();
                case "test06" -> supporter.test06ViewProjects();
                case "test07" -> supporter.test07FilterProjects();
                case "test08" -> supporter.test08ApplyReward();
                case "test09" -> supporter.test09RegisterReview();
                case "test10" -> supporter.test10ConcurrencyControl();
                case "test10b" -> supporter.test10bConcurrencyWithThreads();
                default -> System.out.println("알 수 없는 항목: " + arg);
            }
        }
    }

    /**
     * 자주 나는 오류에 해결 방법을 덧붙여 출력
     */
    private static void explainError(Thread thread, Throwable e) {
        StringBuilder all = new StringBuilder();
        for (Throwable t = e; t != null; t = t.getCause()) {
            all.append(t.getMessage()).append('\n');
        }
        String msg = all.toString();

        System.err.println();
        System.err.println("========== 실행 중 오류 ==========");
        if (msg.contains("DB_PWD")) {
            System.err.println("→ src/main/resources/config/db.properties 의 DB_PWD 를 내 MySQL 비밀번호로 바꿔 주세요.");
        } else if (msg.contains("Access denied")) {
            System.err.println("→ MySQL 아이디/비밀번호가 틀렸습니다. config/db.properties 의 DB_USER, DB_PWD 를 확인하세요.");
        } else if (msg.contains("Unknown database") || msg.contains("doesn't exist")) {
            System.err.println("→ DB 또는 테이블이 없습니다. Workbench 에서 sql/crowdfunding.sql 을 먼저 실행하세요.");
        } else if (msg.contains("Communications link failure") || msg.contains("Connection refused")) {
            System.err.println("→ MySQL 서버에 연결할 수 없습니다. MySQL 이 켜져 있는지, 포트(3306)가 맞는지 확인하세요.");
        } else if (msg.contains("foreign key constraint") || msg.contains("Cannot add or update a child row")) {
            System.err.println("→ 참조하는 데이터가 없습니다. initDB() 를 먼저 실행했는지 확인하세요.");
        }
        System.err.println("==================================");
        e.printStackTrace();
    }
}
