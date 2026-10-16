package crowdfunding.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/**
 * [검사 6·7] 프로젝트 검색 조건. 비어 있는 조건은 MyBatis 동적 쿼리에서 빠진다.
 *
 *   keyword    : 프로젝트명 검색어            (null 이면 조건 없음)
 *   categoryId : 카테고리 번호               (null 이면 전체)
 *   status     : IN_PROGRESS(펀딩중) / CLOSED(펀딩종료) / null(전체)
 *   sort       : LATEST(최신순, 기본) / AMOUNT(모집금액순) / DEADLINE(마감임박순) / LIKES(좋아요순)
 *
 * [담당] B (검사 06·07)
 */
@Getter
@Setter
@ToString
@NoArgsConstructor   // new ProjectSearchCondition() = 조건 없는 검색 (프로젝트 보기 기본)
public class ProjectSearchCondition {

    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_CLOSED = "CLOSED";

    public static final String SORT_LATEST = "LATEST";
    public static final String SORT_AMOUNT = "AMOUNT";
    public static final String SORT_DEADLINE = "DEADLINE";
    public static final String SORT_LIKES = "LIKES";

    private String keyword;
    private Integer categoryId;
    private String status;
    private String sort = SORT_LATEST;
    private LocalDate today;   // '펀딩중/펀딩종료' 판단 기준일. Service 가 채운다.

    public ProjectSearchCondition(Integer categoryId, String status, String keyword) {
        this.categoryId = categoryId;
        this.status = status;
        this.keyword = keyword;
    }

    /** 콘솔 출력용 조건 설명 */
    public String describe() {
        String statusText = STATUS_IN_PROGRESS.equals(status) ? "펀딩중"
                : STATUS_CLOSED.equals(status) ? "펀딩종료" : "전체";
        String sortText = switch (sort == null ? SORT_LATEST : sort) {
            case SORT_AMOUNT -> "모집금액순";
            case SORT_DEADLINE -> "마감임박순";
            case SORT_LIKES -> "좋아요순";
            default -> "최신순";
        };
        return "카테고리=" + (categoryId == null ? "전체" : categoryId)
                + ", 상태=" + statusText
                + ", 검색어=" + (keyword == null || keyword.isBlank() ? "없음" : "'" + keyword + "'")
                + ", 정렬=" + sortText;
    }
}
