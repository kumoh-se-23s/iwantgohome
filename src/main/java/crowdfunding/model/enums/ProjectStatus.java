package crowdfunding.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * project.project_status 컬럼 값 ENUM('in_progress','success','fail')
 * 승인되기 전에는 NULL (DB CHECK 제약: 승인 상태일 때만 값이 있어야 함)
 *
 * 서포터 화면에서는 두 가지로만 보인다 (구현 범위 1.3.2)
 *   펀딩중   = in_progress 이고 종료일이 오늘 이후
 *   펀딩종료 = success / fail (또는 종료일이 지난 것)
 *
 * [담당] 공통 기반
 */
@Getter                    // getValue(), getLabel()
@RequiredArgsConstructor   // (value, label) 생성자
public enum ProjectStatus {
    IN_PROGRESS("in_progress", "진행 중"),
    SUCCESS("success", "펀딩 성공"),
    FAIL("fail", "펀딩 실패");

    private final String value;   // DB 에 저장되는 값
    private final String label;   // 화면에 보여 줄 한글 이름

    public boolean matches(String dbValue) {
        return value.equals(dbValue);
    }

    public static String labelOf(String dbValue) {
        if (dbValue == null) {
            return "-";
        }
        for (ProjectStatus s : values()) {
            if (s.matches(dbValue)) {
                return s.label;
            }
        }
        return dbValue;
    }
}
