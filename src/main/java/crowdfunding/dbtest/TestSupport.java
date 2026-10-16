package crowdfunding.dbtest;

import crowdfunding.exception.BusinessException;
import crowdfunding.view.ResultView;

/**
 * 검사 코드에서 같이 쓰는 도우미.
 *
 * [담당] 공통 기반
 */
final class TestSupport {

    private TestSupport() {
    }

    /**
     * 기능을 한 번 실행하고 결과를 [성공] / [실패] 로 출력한다.
     * 업무 규칙 위반(BusinessException)은 실패 이유만 보여 주고, 그 밖의 오류(DB 연결 등)는 그대로 던진다.
     * @return 성공하면 true
     */
    static boolean attempt(String label, Runnable action) {
        try {
            action.run();
            ResultView.success(label);
            return true;
        } catch (BusinessException e) {
            ResultView.fail(label + "  →  " + e.getMessage());
            return false;
        }
    }
}
