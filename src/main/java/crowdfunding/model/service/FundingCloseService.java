package crowdfunding.model.service;

import crowdfunding.config.AppConfig;
import crowdfunding.config.TransactionManager;
import crowdfunding.model.dao.ProjectMapper;

/**
 * 펀딩 결과 처리 (요구사항 4.1 ~ 4.3)
 * 종료일이 지난 '진행 중' 프로젝트를 총 펀딩 금액 ≥ 목표 금액 이면 success, 아니면 fail 로 바꾼다.
 * <ul>
 *   <li>1차: initDB() 마지막에 한 번 호출 (종료된 시드 프로젝트의 성공/실패 결정)</li>
 *   <li>2차: 서버 시작 시 + 주기적으로(AppConfig.CLOSE_CHECK_INTERVAL_SECONDS) 자동 호출</li>
 * </ul>
 * 성공한 프로젝트의 리워드는 자동 배송된 것으로 간주한다 (요구사항 4.4) → 별도 배송 처리 없음.
 */
public class FundingCloseService {

    /** @return 이번에 성공/실패로 마감된 프로젝트 수 */
    public int closeExpiredProjects() {
        return TransactionManager.execute(session ->
                session.getMapper(ProjectMapper.class).closeExpiredProjects(AppConfig.today()));
    }
}
