package crowdfunding.model.enums;

/**
 * 프로젝트 승인 상태 — project.approval_status ENUM('draft','waiting','approved','reject')
 *
 * <pre>
 *  DRAFT(작성 중) ──신청──▶ WAITING(승인 대기) ──승인──▶ APPROVED(승인 완료, 서포터에게 공개)
 *        ▲                         │
 *        │                       거절(사유 작성)
 *        │                         ▼
 *        └────── 수정 후 재신청 ── REJECT(거절)
 * </pre>
 */
public enum ApprovalStatus implements CodeEnum {
    DRAFT("draft", "작성 중"),
    WAITING("waiting", "승인 대기"),
    APPROVED("approved", "승인 완료"),
    REJECT("reject", "승인 거절");

    private final String code;
    private final String label;

    ApprovalStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    @Override public String getCode() { return code; }
    @Override public String getLabel() { return label; }

    /** 메이커가 내용을 수정하거나 리워드를 추가할 수 있는 상태인가? (작성 중 또는 거절됨) */
    public boolean isEditable() {
        return this == DRAFT || this == REJECT;
    }
}
