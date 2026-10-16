package crowdfunding.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * project.approval_status 컬럼 값 ENUM('draft','waiting','approved','reject')
 *
 * 흐름: draft(작성 중) → [리워드 등록 후 신청] → waiting(승인 대기)
 *       → [관리자 승인] → approved   /   [관리자 거절] → reject → [수정 후 재신청] → waiting
 *
 * ※ 거절 값은 'rejected' 가 아니라 'reject' 다 (crowdfunding.sql 기준)
 *
 * [담당] 공통 기반
 */
@Getter                    // getValue(), getLabel()
@RequiredArgsConstructor   // (value, label) 생성자
public enum ApprovalStatus {
    DRAFT("draft", "작성 중"),
    WAITING("waiting", "승인 대기"),
    APPROVED("approved", "승인"),
    REJECT("reject", "거절");

    private final String value;   // DB 에 저장되는 값
    private final String label;   // 화면에 보여 줄 한글 이름

    public boolean matches(String dbValue) {
        return value.equals(dbValue);
    }

    public static String labelOf(String dbValue) {
        for (ApprovalStatus s : values()) {
            if (s.matches(dbValue)) {
                return s.label;
            }
        }
        return String.valueOf(dbValue);
    }
}
