package crowdfunding.dbtest;

import crowdfunding.config.AppConfig;
import crowdfunding.config.MyBatisConfig;
import crowdfunding.config.TransactionManager;
import crowdfunding.model.dao.CategoryMapper;
import crowdfunding.model.dao.DataInitMapper;
import crowdfunding.model.dao.FundingMapper;
import crowdfunding.model.dao.LikeMapper;
import crowdfunding.model.dao.ProjectMapper;
import crowdfunding.model.dao.ReviewMapper;
import crowdfunding.model.dao.RewardMapper;
import crowdfunding.model.dao.UserMapper;
import crowdfunding.model.domain.Category;
import crowdfunding.model.domain.Funding;
import crowdfunding.model.domain.Project;
import crowdfunding.model.domain.Review;
import crowdfunding.model.domain.Reward;
import crowdfunding.model.domain.Supporter;
import crowdfunding.model.domain.User;
import crowdfunding.model.enums.ApprovalStatus;
import crowdfunding.model.enums.ProjectStatus;
import crowdfunding.model.enums.Role;
import crowdfunding.model.service.FundingCloseService;
import org.apache.ibatis.session.SqlSession;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * initDB() — 검사에 필요한 사전 데이터를 한 번에 입력한다. (1차 검사, 2차 시연 공통)
 * <p>
 * 1) 모든 테이블을 TRUNCATE → AUTO_INCREMENT 가 1부터 다시 시작하므로 PK 가 항상 아래 상수와 같다.
 * 2) 날짜는 실행한 날(오늘) 기준 상대값 → 언제 실행해도 '진행 중', '종료(성공/실패)' 프로젝트가 만들어진다.
 * 3) 마지막에 FundingCloseService 로 종료일이 지난 프로젝트를 성공/실패 처리한다.
 *
 * <pre>
 *  번호 | 프로젝트                 | 메이커  | 카테고리 | 상태
 *  1    | 접이식 블루투스 키보드   | maker01 | 테크·가전 | 펀딩중   ← 리워드1 [얼리버드] 잔여 1개 (test10 동시성)
 *  2    | 수제 그래놀라 정기배송   | maker01 | 푸드      | 종료-성공 ← sup01 리뷰 작성 대상 (test09)
 *  3    | 휴대용 미니 빔프로젝터   | maker01 | 테크·가전 | 종료-실패
 *  4    | 제주 감귤 착즙 주스      | maker02 | 푸드      | 펀딩중   ← sup01 리워드 신청 대상 (test08)
 *  5    | 스마트 식물 재배기       | maker02 | 테크·가전 | 펀딩중(마감임박) ← 리워드9 한정 2개 (스레드 동시성 테스트)
 *  6    | 비건 단백질 쿠키         | maker02 | 푸드      | 승인 대기
 *  7    | 전통 발효 식초           | maker01 | 푸드      | 승인 거절 (2차: 수정 후 재신청 시연용)
 *  8    | 노이즈캔슬링 수면 이어폰 | maker02 | 테크·가전 | 종료-성공 (리뷰 3개)
 *  (9   | test02 에서 새로 만드는 프로젝트)
 * </pre>
 */
public class DataInitializer {

    // ---------------- 계정 (아이디 5~10자, 비밀번호 8~16자) ----------------
    public static final String ADMIN_ID = "admin01";
    public static final String ADMIN_PW = "admin1234";
    public static final String MAKER1_ID = "maker01";      // 검사용 메이커 (프로젝트 1, 2, 3, 7 — 후원 기록 있음)
    public static final String MAKER2_ID = "maker02";
    public static final String MAKER_PW = "maker1234";
    public static final String SUPPORTER_PW = "pass1234";  // sup01 ~ sup08 공통

    // ---------------- PK (initDB 직후 항상 이 번호) ----------------
    public static final int CATEGORY_TECH = 1;   // 테크·가전
    public static final int CATEGORY_FOOD = 2;   // 푸드

    public static final int P_KEYBOARD = 1;
    public static final int P_GRANOLA = 2;
    public static final int P_PROJECTOR = 3;
    public static final int P_JUICE = 4;
    public static final int P_PLANTER = 5;
    public static final int P_COOKIE = 6;
    public static final int P_VINEGAR = 7;
    public static final int P_EARPHONE = 8;

    public static final int R_KEYBOARD_EARLY = 1;     // 한정 3개, 2개 신청됨 → 잔여 1
    public static final int R_JUICE_10 = 7;           // 한정 50개
    public static final int R_JUICE_30 = 8;           // 무제한
    public static final int R_PLANTER_LIMITED = 9;    // 한정 2개, 신청 0

    private static final String[] TABLES = {
            "review", "project_like", "funding", "reward", "project", "category", "supporter", "`user`"
    };

    public void initDB() {
        System.out.println("[initDB] 테이블 초기화 중...");
        resetTables();

        LocalDate today = AppConfig.today();
        TransactionManager.run(session -> seed(session, today));

        int closed = new FundingCloseService().closeExpiredProjects();
        System.out.println("[initDB] 사전 데이터 입력 완료 (기준일 " + today + ")");
        System.out.println("         사용자 11명(관리자 1, 메이커 2, 서포터 8), 카테고리 2개, 프로젝트 8개, 리워드 16개");
        System.out.println("         후원 15건, 좋아요 12건, 리뷰 4건, 펀딩 종료 처리 " + closed + "건(성공 2, 실패 1)");
        System.out.println("         계정: " + ADMIN_ID + "/" + ADMIN_PW + ", " + MAKER1_ID + "·" + MAKER2_ID + "/" + MAKER_PW
                + ", sup01~sup08/" + SUPPORTER_PW);
    }

    /** FK 검사를 잠시 끄고 모든 테이블을 비운다 (같은 커넥션에서 실행해야 하므로 SqlSession 하나로 처리) */
    private void resetTables() {
        try (SqlSession session = MyBatisConfig.getSqlSessionFactory().openSession(true)) {
            DataInitMapper mapper = session.getMapper(DataInitMapper.class);
            mapper.disableForeignKeyChecks();
            try {
                for (String table : TABLES) {
                    mapper.truncateTable(table);
                }
            } finally {
                mapper.enableForeignKeyChecks();   // 커넥션 풀로 돌아가기 전에 반드시 원상복구
            }
        }
    }

    private void seed(SqlSession s, LocalDate d) {
        UserMapper um = s.getMapper(UserMapper.class);
        CategoryMapper cm = s.getMapper(CategoryMapper.class);
        ProjectMapper pm = s.getMapper(ProjectMapper.class);
        RewardMapper rm = s.getMapper(RewardMapper.class);
        FundingMapper fm = s.getMapper(FundingMapper.class);
        LikeMapper lm = s.getMapper(LikeMapper.class);
        ReviewMapper vm = s.getMapper(ReviewMapper.class);

        // ===================== 사용자 =====================
        user(um, ADMIN_ID, ADMIN_PW, Role.ADMIN, "관리자", "010-1000-0001");
        int maker1 = user(um, MAKER1_ID, MAKER_PW, Role.MAKER, "김민수", "010-2000-0001");
        int maker2 = user(um, MAKER2_ID, MAKER_PW, Role.MAKER, "이수진", "010-2000-0002");
        int sup1 = supporter(um, "sup01", "박지훈", "010-3000-0001", "서울특별시 강남구 테헤란로 152", LocalDate.of(2001, 3, 15));
        int sup2 = supporter(um, "sup02", "최서연", "010-3000-0002", "경상북도 구미시 대학로 61", LocalDate.of(2002, 5, 2));
        int sup3 = supporter(um, "sup03", "정민준", "010-3000-0003", "부산광역시 해운대구 센텀중앙로 79", LocalDate.of(1999, 11, 23));
        int sup4 = supporter(um, "sup04", "강하은", "010-3000-0004", "대구광역시 중구 동성로 25", LocalDate.of(2000, 8, 9));
        int sup5 = supporter(um, "sup05", "조예준", "010-3000-0005", "인천광역시 연수구 송도과학로 32", LocalDate.of(1998, 1, 30));
        int sup6 = supporter(um, "sup06", "윤서아", "010-3000-0006", "대전광역시 유성구 대학로 99", LocalDate.of(2003, 4, 17));
        int sup7 = supporter(um, "sup07", "장도윤", "010-3000-0007", "광주광역시 북구 용봉로 77", null);
        int sup8 = supporter(um, "sup08", "임지우", "010-3000-0008", "경기도 수원시 팔달구 효원로 1", LocalDate.of(2002, 7, 7));

        // ===================== 카테고리 (2개) =====================
        int tech = category(cm, "테크·가전");
        int food = category(cm, "푸드");
        check(tech == CATEGORY_TECH && food == CATEGORY_FOOD, "카테고리 PK");

        // ===================== 프로젝트 1: 펀딩중, 한정 리워드 잔여 1개 =====================
        int p1 = project(pm, maker1, tech, "접이식 블루투스 키보드",
                "가방에 쏙 들어가는 접이식 블루투스 키보드. 기기 3대 멀티페어링, 1회 충전 3개월 사용.",
                ApprovalStatus.APPROVED, null, d.minusDays(10), d.plusDays(20), 1_000_000, d.minusDays(12));
        int r1 = reward(rm, p1, "[얼리버드] 키보드 1개", "정가 대비 20% 할인 (한정 3개)", 39_000, 3, d.plusDays(35));
        int r2 = reward(rm, p1, "[일반] 키보드 1개", "키보드 1개 + 전용 파우치", 49_000, null, d.plusDays(35));
        fund(fm, rm, sup1, p1, r1, at(d.minusDays(9), 10));
        fund(fm, rm, sup2, p1, r1, at(d.minusDays(8), 14));
        fund(fm, rm, sup3, p1, r2, at(d.minusDays(5), 20));
        like(lm, p1, sup1, sup2, sup4, sup5);

        // ===================== 프로젝트 2: 종료 → 성공 (119,000 / 100,000) =====================
        int p2 = project(pm, maker1, food, "수제 그래놀라 정기배송",
                "국산 귀리와 견과류로 매주 굽는 무설탕 그래놀라. 원하는 기간만큼 정기배송.",
                ApprovalStatus.APPROVED, null, d.minusDays(40), d.minusDays(5), 100_000, d.minusDays(42));
        int r3 = reward(rm, p2, "그래놀라 1개월분", "500g x 4봉", 25_000, 10, d.plusDays(3));
        int r4 = reward(rm, p2, "그래놀라 3개월분", "500g x 12봉 + 전용 밀폐용기", 69_000, 5, d.plusDays(3));
        fund(fm, rm, sup2, p2, r3, at(d.minusDays(30), 9));
        fund(fm, rm, sup4, p2, r3, at(d.minusDays(25), 13));
        fund(fm, rm, sup1, p2, r4, at(d.minusDays(20), 18));
        like(lm, p2, sup4);
        review(vm, sup2, p2, 5, "바삭하고 달지 않아서 아침마다 잘 먹고 있어요!", at(d.minusDays(3), 8));

        // ===================== 프로젝트 3: 종료 → 실패 (159,000 / 3,000,000) =====================
        int p3 = project(pm, maker1, tech, "휴대용 미니 빔프로젝터",
                "스마트폰 크기의 미니 빔프로젝터. 캠핑장에서도 100인치 화면.",
                ApprovalStatus.APPROVED, null, d.minusDays(45), d.minusDays(3), 3_000_000, d.minusDays(47));
        int r5 = reward(rm, p3, "빔프로젝터 기본 패키지", "본체 + 리모컨 + 충전 케이블", 159_000, 20, d.plusDays(30));
        reward(rm, p3, "빔프로젝터 + 삼각대 세트", "기본 패키지 + 접이식 삼각대", 189_000, 10, d.plusDays(30));
        fund(fm, rm, sup3, p3, r5, at(d.minusDays(40), 11));

        // ===================== 프로젝트 4: 펀딩중 (test08 리워드 신청 대상) =====================
        int p4 = project(pm, maker2, food, "제주 감귤 착즙 주스",
                "제주 노지 감귤을 그대로 짜낸 100% 착즙 주스. 무가당·무첨가.",
                ApprovalStatus.APPROVED, null, d.minusDays(5), d.plusDays(25), 500_000, d.minusDays(7));
        int r7 = reward(rm, p4, "착즙 주스 10병", "200ml x 10병", 15_000, 50, d.plusDays(40));
        int r8 = reward(rm, p4, "착즙 주스 30병", "200ml x 30병 + 보냉백", 39_000, null, d.plusDays(40));
        fund(fm, rm, sup4, p4, r7, at(d.minusDays(4), 10));
        fund(fm, rm, sup5, p4, r7, at(d.minusDays(3), 15));
        fund(fm, rm, sup6, p4, r8, at(d.minusDays(2), 21));
        like(lm, p4, sup1, sup3);

        // ===================== 프로젝트 5: 펀딩중·마감임박 (한정 2개 — 스레드 동시성 테스트) =====================
        int p5 = project(pm, maker2, tech, "스마트 식물 재배기",
                "LED 조명과 자동 급수로 실내에서 허브와 채소를 키우는 스마트 재배기.",
                ApprovalStatus.APPROVED, null, d.minusDays(3), d.plusDays(7), 2_000_000, d.minusDays(5));
        int r9 = reward(rm, p5, "[한정] 얼리버드 재배기", "본체 + 씨앗 키트 3종 (한정 2개)", 99_000, 2, d.plusDays(21));
        int r10 = reward(rm, p5, "재배기 기본 구성", "본체 + 씨앗 키트 1종", 129_000, null, d.plusDays(21));
        fund(fm, rm, sup7, p5, r10, at(d.minusDays(2), 12));
        like(lm, p5, sup2, sup3, sup6);

        // ===================== 프로젝트 6: 승인 대기 =====================
        int p6 = project(pm, maker2, food, "비건 단백질 쿠키",
                "식물성 단백질 12g, 버터·달걀 없이 만든 비건 쿠키.",
                ApprovalStatus.WAITING, null, d, d.plusDays(30), 300_000, d.minusDays(1));
        reward(rm, p6, "쿠키 6개입", "초코·말차·오리지널 각 2개", 12_000, 100, d.plusDays(45));
        reward(rm, p6, "쿠키 12개입", "6개입 x 2 + 텀블러", 22_000, null, d.plusDays(45));

        // ===================== 프로젝트 7: 승인 거절 (2차 '수정 후 재신청' 시연용) =====================
        int p7 = project(pm, maker1, food, "전통 발효 식초",
                "3년 숙성 현미 발효 식초.",
                ApprovalStatus.REJECT, "리워드 구성과 제공 시기 설명이 부족합니다. 상세 설명을 보완해 주세요.",
                d.plusDays(1), d.plusDays(31), 400_000, d.minusDays(3));
        reward(rm, p7, "발효 식초 500ml", null, 18_000, 30, null);
        reward(rm, p7, "발효 식초 선물세트", null, 45_000, 10, null);

        // ===================== 프로젝트 8: 종료 → 성공 (436,000 / 300,000), 리뷰 3개 =====================
        int p8 = project(pm, maker2, tech, "노이즈캔슬링 수면 이어폰",
                "옆으로 누워도 편한 초소형 수면 이어폰. 소음 차단 + 수면 사운드 내장.",
                ApprovalStatus.APPROVED, null, d.minusDays(60), d.minusDays(20), 300_000, d.minusDays(62));
        int r15 = reward(rm, p8, "수면 이어폰 1개", "이어폰 + 충전 케이스", 89_000, 5, d.minusDays(10));
        int r16 = reward(rm, p8, "수면 이어폰 2개 세트", "이어폰 2개 + 충전 케이스 2개", 169_000, 3, d.minusDays(10));
        fund(fm, rm, sup5, p8, r15, at(d.minusDays(55), 10));
        fund(fm, rm, sup6, p8, r15, at(d.minusDays(50), 16));
        fund(fm, rm, sup7, p8, r15, at(d.minusDays(45), 19));
        fund(fm, rm, sup8, p8, r16, at(d.minusDays(40), 22));
        like(lm, p8, sup5, sup6);
        review(vm, sup5, p8, 4, "소음 차단은 좋은데 배터리가 조금 아쉬워요.", at(d.minusDays(9), 12));
        review(vm, sup6, p8, 5, "옆으로 누워도 귀가 안 아파요. 강력 추천!", at(d.minusDays(8), 23));
        review(vm, sup7, p8, 3, null, at(d.minusDays(7), 9));

        check(p1 == P_KEYBOARD && p2 == P_GRANOLA && p3 == P_PROJECTOR && p4 == P_JUICE
                && p5 == P_PLANTER && p6 == P_COOKIE && p7 == P_VINEGAR && p8 == P_EARPHONE, "프로젝트 PK");
        check(r1 == R_KEYBOARD_EARLY && r7 == R_JUICE_10 && r8 == R_JUICE_30 && r9 == R_PLANTER_LIMITED, "리워드 PK");
        check(sup1 > 0 && sup8 > 0 && r10 > 0, "서포터 PK");
    }

    // ------------------------------------------------------------ 입력 도우미

    private int user(UserMapper um, String loginId, String pw, Role role, String name, String phone) {
        User user = new User(loginId, pw, role, name, phone);
        um.insertUser(user);
        return user.getUserId();
    }

    private int supporter(UserMapper um, String loginId, String name, String phone, String address, LocalDate birth) {
        int userId = user(um, loginId, SUPPORTER_PW, Role.SUPPORTER, name, phone);
        um.insertSupporter(new Supporter(userId, address, birth));
        return userId;
    }

    private int category(CategoryMapper cm, String name) {
        Category category = new Category(name);
        cm.insertCategory(category);
        return category.getCategoryId();
    }

    /** 승인된 프로젝트는 일단 '진행 중'으로 넣고, 종료일이 지난 것은 initDB 마지막에 성공/실패 처리된다 */
    private int project(ProjectMapper pm, int makerId, int categoryId, String name, String description,
                        ApprovalStatus approval, String rejectReason, LocalDate start, LocalDate end,
                        int target, LocalDate createdDay) {
        Project p = new Project(name, description, categoryId, target, start, end);
        p.setMakerId(makerId);
        p.setApprovalStatus(approval);
        p.setRejectReason(rejectReason);
        p.setProjectStatus(approval == ApprovalStatus.APPROVED ? ProjectStatus.IN_PROGRESS : null);
        p.setCreatedAt(at(createdDay, 9));
        pm.insertProject(p);
        return p.getProjectId();
    }

    private int reward(RewardMapper rm, int projectId, String name, String description, int price,
                       Integer amount, LocalDate deliveryDay) {
        Reward r = new Reward(projectId, name, description, price, amount,
                deliveryDay == null ? null : deliveryDay.atStartOfDay());
        rm.insertReward(r);
        return r.getRewardId();
    }

    /** 후원 기록 + 리워드 신청 수량 증가 (실제 서비스와 같은 매퍼 메소드 사용) */
    private void fund(FundingMapper fm, RewardMapper rm, int userId, int projectId, int rewardId, LocalDateTime when) {
        check(rm.increaseAppliedCount(rewardId) == 1, "리워드 " + rewardId + " 수량");
        Funding f = new Funding(projectId, rewardId);
        f.setUserId(userId);
        f.setFundedDate(when);
        fm.insertFunding(f);
    }

    private void like(LikeMapper lm, int projectId, int... userIds) {
        for (int userId : userIds) {
            lm.insertLike(projectId, userId);
        }
    }

    private void review(ReviewMapper vm, int userId, int projectId, int rating, String content, LocalDateTime when) {
        Review r = new Review(projectId, rating, content);
        r.setUserId(userId);
        r.setReviewAt(when);
        vm.insertReview(r);
    }

    private static LocalDateTime at(LocalDate day, int hour) {
        return day.atTime(hour, 0);
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            throw new IllegalStateException("[initDB] " + what + " 가 예상과 다릅니다. 테이블이 제대로 비워졌는지 확인하세요.");
        }
    }
}
