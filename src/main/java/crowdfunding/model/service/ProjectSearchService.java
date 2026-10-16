package crowdfunding.model.service;

import crowdfunding.config.AppConfig;
import crowdfunding.exception.BusinessException;
import crowdfunding.model.dao.ProjectMapper;
import crowdfunding.model.dto.ProjectSearchCondition;
import crowdfunding.model.dto.ProjectSummary;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.util.List;
import java.util.Set;

/**
 * 서포터의 프로젝트 보기 · 검색
 *
 *   [검사 6] 조건 없이 search() → 승인된 프로젝트 전체 (최신순)
 *   [검사 7] 카테고리 · 진행/마감 조건으로 search() → MyBatis 동적 쿼리 (ProjectMapper.xml 의 search)
 *
 * [담당] B (검사 06·07)
 */
@RequiredArgsConstructor
public class ProjectSearchService {

    private static final Set<String> STATUSES =
            Set.of(ProjectSearchCondition.STATUS_IN_PROGRESS, ProjectSearchCondition.STATUS_CLOSED);
    private static final Set<String> SORTS = Set.of(
            ProjectSearchCondition.SORT_LATEST, ProjectSearchCondition.SORT_AMOUNT,
            ProjectSearchCondition.SORT_DEADLINE, ProjectSearchCondition.SORT_LIKES);

    private final SqlSessionFactory sqlSessionFactory;   // Main 에서 생성자로 넣어 준다
    private final ProjectCloseService closeService;

    public List<ProjectSummary> search(ProjectSearchCondition condition) {
        ProjectSearchCondition c = condition != null ? condition : new ProjectSearchCondition();

        // 정해진 값만 허용 (이상한 값이 SQL 로 흘러가지 않도록)
        if (c.getStatus() != null && !STATUSES.contains(c.getStatus())) {
            throw new BusinessException("진행 상태 조건은 IN_PROGRESS(펀딩중) 또는 CLOSED(펀딩종료)만 가능합니다.");
        }
        if (c.getSort() == null) {
            c.setSort(ProjectSearchCondition.SORT_LATEST);
        } else if (!SORTS.contains(c.getSort())) {
            throw new BusinessException("정렬은 LATEST / AMOUNT / DEADLINE / LIKES 중 하나만 가능합니다.");
        }
        if (c.getKeyword() != null) {
            c.setKeyword(c.getKeyword().trim());
        }

        closeService.closeExpiredProjects();   // 기간이 끝난 프로젝트를 먼저 마감 처리
        c.setToday(AppConfig.today());

        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            return session.getMapper(ProjectMapper.class).search(c);
        }
    }
}
