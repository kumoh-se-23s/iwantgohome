package crowdfunding.model.service;

import crowdfunding.config.AppConfig;
import crowdfunding.model.dao.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * 펀딩 기간이 끝난 프로젝트 마감 처리 (과제 4.1 ~ 4.3)
 *
 * 종료일이 지난 '진행 중' 프로젝트를
 *   총 후원 금액 ≥ 목표 금액 → success (펀딩 성공)
 *   그렇지 않으면           → fail    (펀딩 실패)
 * 로 바꾼다.
 *
 * DB 가 날짜가 지났다고 알아서 바꿔 주지는 않으므로, 상태가 중요한 기능
 * (펀딩 현황, 프로젝트 보기·필터링, 리뷰 등록)이 시작할 때 이 메소드를 먼저 호출한다.
 *
 * [담당] 공통 기반
 */
@RequiredArgsConstructor
public class ProjectCloseService {

    private final SqlSessionFactory sqlSessionFactory;   // Main 에서 생성자로 넣어 준다

    /** @return 이번에 마감 처리된 프로젝트 수 */
    public int closeExpiredProjects() {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            try {
                int closed = session.getMapper(ProjectMapper.class).closeExpiredProjects(AppConfig.today());
                session.commit();
                return closed;
            } catch (RuntimeException e) {
                session.rollback();
                throw e;
            }
        }
    }
}
