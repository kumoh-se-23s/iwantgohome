package crowdfunding.model.enums;

/**
 * 서포터가 보는 프로젝트 상태 (검색 필터용, DB 컬럼 아님)
 * 과제 조건: "서포터가 바라보는 관점에서의 프로젝트는 펀딩중/펀딩종료 두 가지 상태를 가진다."
 */
public enum FundingState {
    ALL("전체"),
    OPEN("펀딩중"),
    CLOSED("펀딩종료");

    private final String label;

    FundingState(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
