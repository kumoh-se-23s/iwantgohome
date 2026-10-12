package crowdfunding.model.enums;

/**
 * 서포터 프로젝트 목록 정렬 기준 (2차 13번: 모집금액순, 마감임박순, 좋아요순)
 * 실제 ORDER BY 는 ProjectMapper.xml 의 &lt;choose&gt; 에 고정 문자열로 작성되어 있어 SQL 인젝션 위험이 없다.
 */
public enum ProjectSort {
    LATEST("최신순"),
    AMOUNT("모집금액순"),
    DEADLINE("마감임박순"),
    LIKES("좋아요순");

    private final String label;

    ProjectSort(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
