package crowdfunding;

import crowdfunding.dbtest.AdminTest;
import crowdfunding.dbtest.DataInitializer;
import crowdfunding.dbtest.DbTestSupport;
import crowdfunding.dbtest.MakerTest;
import crowdfunding.dbtest.SupporterTest;

/**
 * ================== 1차 DB 파트 검사 진입점 ==================
 *
 * 검사 방법 (과제 안내):
 *   initDB 실행 → initDB 주석 → 1번 테스트 주석 제거 → 실행 → 주석 → 2번 테스트 주석 제거 → 실행 ...
 *   테이블 변화는 MySQL Workbench 에서 확인 (각 테스트가 확인용 SQL 도 함께 출력함)
 *
 * 검사 코드는 2차 서버와 같은 컨트롤러 → 서비스 → MyBatis 매퍼를 그대로 호출한다.
 * 그래서 1차에서 검사받은 코드가 2차에서도 그대로 쓰인다. (2차는 ServerMain / ClientMain 실행)
 *
 * (선택) 주석 대신 실행 인자로도 실행 가능:  java crowdfunding.Main initDB test01 test02 ...
 */
public class Main {

    private static final DataInitializer dataInitializer = new DataInitializer();
    private static final MakerTest makerTest = new MakerTest();
    private static final AdminTest adminTest = new AdminTest();
    private static final SupporterTest supporterTest = new SupporterTest();

    public static void main(String[] args) {
        if (args.length > 0) {
            runByName(args);
            return;
        }

        //    initDB();                                       //  검사에 필요한 사전 데이터를 insert하는 메소드
        // 검사할 항목 하나만 주석 해제하여 실행
        // makerTest.test01ViewFundingStatus();               // MAKER     - 프로젝트별 펀딩 현황 보기
        // makerTest.test02RegisterProject();                 // MAKER     - 프로젝트 기본 정보 등록
        // makerTest.test03RegisterReward();                  // MAKER     - 리워드 등록 및 프로젝트 신청
        // adminTest.test04ApproveProject();                  // ADMIN     - 프로젝트 등록 승인
        // supporterTest.test05UpdateProfile();               // SUPPORTER - 개인정보 수정
        // supporterTest.test06ViewProjects();                // SUPPORTER - 프로젝트 보기
        // supporterTest.test07FilterProjects();              // SUPPORTER - 프로젝트 필터링
        // supporterTest.test08ApplyReward();                 // SUPPORTER - 리워드 신청
        // supporterTest.test09RegisterReview();              // SUPPORTER - 리뷰 및 별점 등록
        // supporterTest.test10ConcurrencyControl();          // SUPPORTER - 제공 가능 수량 동시성 제어
        // supporterTest.test10bConcurrencyWithThreads();     // (선택) 스레드 6개 동시 신청 테스트

        if (!initDbRan && DbTestSupport.getExecutedCount() == 0) {
            System.out.println("실행할 검사 항목의 주석을 해제하세요. (처음이면 initDB() 부터)");
        }
    }

    private static boolean initDbRan = false;

    /** 검사에 필요한 사전 데이터를 insert 하는 메소드 */
    static void initDB() {
        initDbRan = true;
        dataInitializer.initDB();
    }

    private static void runByName(String[] names) {
        for (String name : names) {
            switch (name) {
                case "initDB": initDB(); break;
                case "test01": makerTest.test01ViewFundingStatus(); break;
                case "test02": makerTest.test02RegisterProject(); break;
                case "test03": makerTest.test03RegisterReward(); break;
                case "test04": adminTest.test04ApproveProject(); break;
                case "test05": supporterTest.test05UpdateProfile(); break;
                case "test06": supporterTest.test06ViewProjects(); break;
                case "test07": supporterTest.test07FilterProjects(); break;
                case "test08": supporterTest.test08ApplyReward(); break;
                case "test09": supporterTest.test09RegisterReview(); break;
                case "test10": supporterTest.test10ConcurrencyControl(); break;
                case "test10b": supporterTest.test10bConcurrencyWithThreads(); break;
                default: System.out.println("알 수 없는 항목: " + name);
            }
        }
    }
}
