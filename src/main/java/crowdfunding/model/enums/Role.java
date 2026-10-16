package crowdfunding.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * user.role 컬럼 값 ENUM('supporter','maker','admin')
 *
 * DB 에는 소문자 문자열이 저장되어 있어서, 도메인 클래스는 String 으로 받고
 * 비교할 때 이 enum 을 쓴다.  예) Role.MAKER.matches(user.getRole())
 *
 * [담당] 공통 기반
 */
@Getter                    // getValue(), getLabel()
@RequiredArgsConstructor   // (value, label) 생성자
public enum Role {
    SUPPORTER("supporter", "서포터"),
    MAKER("maker", "메이커"),
    ADMIN("admin", "관리자");

    private final String value;   // DB 에 저장되는 값
    private final String label;   // 화면에 보여 줄 한글 이름

    /** DB 값이 이 상태와 같은지 */
    public boolean matches(String dbValue) {
        return value.equals(dbValue);
    }

    /** DB 값 → 한글 이름 (모르는 값이면 그대로) */
    public static String labelOf(String dbValue) {
        for (Role r : values()) {
            if (r.matches(dbValue)) {
                return r.label;
            }
        }
        return String.valueOf(dbValue);
    }
}
