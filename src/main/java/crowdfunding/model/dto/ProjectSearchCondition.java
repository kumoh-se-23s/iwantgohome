package crowdfunding.model.dto;

import crowdfunding.model.enums.FundingState;
import crowdfunding.model.enums.ProjectSort;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 서포터 프로젝트 검색 조건 → ProjectMapper.xml 의 MyBatis 동적 쿼리(searchApproved)에 그대로 전달된다.
 * 값이 null 인 조건은 WHERE 절에서 빠진다.
 *
 * <pre>
 *  keyword    : 프로젝트명 검색어 (부분 일치)
 *  categoryId : 카테고리 번호
 *  state      : ALL / OPEN(펀딩중) / CLOSED(펀딩종료)
 *  sort       : LATEST / AMOUNT(모집금액순) / DEADLINE(마감임박순) / LIKES(좋아요순)
 * </pre>
 */
public class ProjectSearchCondition implements Serializable {
    private static final long serialVersionUID = 1L;

    private String keyword;
    private Integer categoryId;
    private FundingState state = FundingState.ALL;
    private ProjectSort sort = ProjectSort.LATEST;
    /** 기준 날짜 — 클라이언트가 아니라 서버(ProjectQueryService)가 채운다 */
    private LocalDate today;

    public ProjectSearchCondition() {
    }

    public ProjectSearchCondition(String keyword, Integer categoryId, FundingState state, ProjectSort sort) {
        this.keyword = keyword;
        this.categoryId = categoryId;
        this.state = state;
        this.sort = sort;
    }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public FundingState getState() { return state; }
    public void setState(FundingState state) { this.state = state; }
    public ProjectSort getSort() { return sort; }
    public void setSort(ProjectSort sort) { this.sort = sort; }
    public LocalDate getToday() { return today; }
    public void setToday(LocalDate today) { this.today = today; }

    @Override
    public String toString() {
        return "검색어=" + (keyword == null ? "(없음)" : "'" + keyword + "'")
                + ", 카테고리=" + (categoryId == null ? "전체" : categoryId)
                + ", 상태=" + (state == null ? FundingState.ALL.getLabel() : state.getLabel())
                + ", 정렬=" + (sort == null ? ProjectSort.LATEST.getLabel() : sort.getLabel());
    }
}
