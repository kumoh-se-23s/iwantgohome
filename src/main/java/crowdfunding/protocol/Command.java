package crowdfunding.protocol;

/**
 * 클라이언트 → 서버 요청 종류 (2차 TCP 통신에서 사용).
 * 각 Command 가 어떤 컨트롤러 메소드로 연결되는지는 {@link crowdfunding.controller.RequestDispatcher} 참고.
 * 괄호 안은 Request 에 담는 payload 타입.
 */
public enum Command {

    // ---------------- 공통 ----------------
    LOGIN,                       // (LoginForm)
    LOGOUT,                      // (없음)
    GET_CATEGORIES,              // (없음)
    SEARCH_PROJECTS,             // (ProjectSearchCondition)  ★ MyBatis 동적 쿼리
    GET_PROJECT_DETAIL,          // (Integer 프로젝트 번호)
    GET_PROJECT_REVIEWS,         // (Integer 프로젝트 번호)

    // ---------------- 메이커 ----------------
    MAKER_REGISTER_PROJECT,      // (Project)
    MAKER_ADD_REWARD,            // (Reward — projectId 포함)
    MAKER_SUBMIT_PROJECT,        // (Integer 프로젝트 번호)  신청 / 거절 후 재신청
    MAKER_MY_PROJECTS,           // (없음)
    MAKER_FUNDING_STATUS,        // (Integer 프로젝트 번호)

    // ---------------- 관리자 ----------------
    ADMIN_WAITING_PROJECTS,      // (없음)
    ADMIN_APPROVE_PROJECT,       // (Integer 프로젝트 번호)

    // ---------------- 서포터 ----------------
    SUPPORTER_GET_PROFILE,       // (없음)
    SUPPORTER_UPDATE_PROFILE,    // (ProfileForm)
    SUPPORTER_APPLY_REWARD,      // (Funding — projectId, rewardId)  ★ 동시성 제어
    SUPPORTER_REGISTER_REVIEW,   // (Review — projectId, rating, content)

    // ---------------- [2차에서 추가] ----------------
    // SIGN_UP,                  // (SignUpForm)            서포터 회원가입
    // MAKER_UPDATE_PROJECT,     // (Project)               거절된 프로젝트 수정
    // ADMIN_REJECT_PROJECT,     // (RejectForm)            거절 + 사유
    // ADMIN_FUNDING_OVERVIEW,   // (AdminSortCondition)    전체 펀딩 현황 + 정렬
    // ADMIN_FUNDING_STATUS,     // (Integer)               프로젝트별 펀딩 현황
    // SUPPORTER_CANCEL_FUNDING, // (Integer)               후원 취소
    // SUPPORTER_TOGGLE_LIKE,    // (Integer)               좋아요 / 좋아요 취소
    // SUPPORTER_MY_FUNDINGS,    // (없음)                  MyPage 후원 내역
}
