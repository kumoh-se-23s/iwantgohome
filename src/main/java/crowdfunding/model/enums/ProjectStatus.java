package crowdfunding.model.enums;

/**
 * 펀딩 진행 상태 — project.project_status ENUM('in_progress','success','fail')
 * 승인 전에는 NULL, 승인되는 순간 IN_PROGRESS,
 * 종료일이 지나면 FundingCloseService 가 목표 금액 달성 여부로 SUCCESS / FAIL 결정.
 */
public enum ProjectStatus implements CodeEnum {
    IN_PROGRESS("in_progress", "펀딩 진행 중"),
    SUCCESS("success", "펀딩 성공"),
    FAIL("fail", "펀딩 실패");

    private final String code;
    private final String label;

    ProjectStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    @Override public String getCode() { return code; }
    @Override public String getLabel() { return label; }
}
