package crowdfunding.model.dao;

import crowdfunding.model.domain.Project;
import crowdfunding.model.dto.ProjectSearchCondition;
import crowdfunding.model.dto.ProjectSummary;
import crowdfunding.model.enums.ApprovalStatus;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/** project 테이블 DAO — SQL 은 resources/mapper/ProjectMapper.xml */
public interface ProjectMapper {

    /** INSERT 후 생성된 project_id 가 project.projectId 에 채워진다 */
    int insertProject(Project project);

    Project findById(int projectId);

    ProjectSummary findSummaryById(@Param("projectId") int projectId, @Param("today") LocalDate today);

    /** ★ 서포터 프로젝트 검색 — MyBatis 동적 쿼리 (검색어/카테고리/진행·마감 필터 + 정렬) */
    List<ProjectSummary> searchApproved(ProjectSearchCondition condition);

    /** 메이커 본인의 모든 프로젝트 (작성 중 ~ 종료) */
    List<ProjectSummary> findByMaker(@Param("makerId") int makerId, @Param("today") LocalDate today);

    /** 관리자: 승인 상태별 목록 (예: WAITING = 승인 대기) */
    List<ProjectSummary> findByApprovalStatus(@Param("status") ApprovalStatus status, @Param("today") LocalDate today);

    /** 승인 신청: draft/reject → waiting (다른 상태면 0행 변경) */
    int submit(int projectId);

    /** 승인: waiting → approved + 펀딩 진행 중 (다른 상태면 0행 변경) */
    int approve(int projectId);

    /** 종료일이 지난 진행 중 프로젝트를 목표 달성 여부에 따라 success / fail 로 변경. 변경된 행 수 반환 */
    int closeExpiredProjects(LocalDate today);
}
