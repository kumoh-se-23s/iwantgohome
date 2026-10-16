package crowdfunding.model.dao;

import crowdfunding.model.domain.Project;
import crowdfunding.model.dto.FundingStatus;
import crowdfunding.model.dto.ProjectSearchCondition;
import crowdfunding.model.dto.ProjectSummary;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * project 테이블 SQL (resources/mapper/ProjectMapper.xml)
 * 여러 명이 같이 쓰는 파일이라 담당 구역별로 나눠 두었다.
 */
public interface ProjectMapper {

    // ===== 공통 =====
    Project findById(@Param("projectId") int projectId);

    /** 종료일이 지난 진행 중 프로젝트를 성공/실패로 마감. 바뀐 행 수 반환 */
    int closeExpiredProjects(@Param("today") LocalDate today);

    // ===== A: 등록 · 신청 · 승인 (검사 02·03·04) =====
    /** 기본 정보 등록 (draft). 생성된 PK 는 project.projectId 에 채워진다 */
    int insertBasicInfo(Project project);

    /** draft/reject → waiting (승인 신청) */
    int submit(@Param("projectId") int projectId);

    /** waiting → approved + in_progress (관리자 승인) */
    int approve(@Param("projectId") int projectId);

    /** 승인 상태가 approvalStatus 인 가장 최근 프로젝트 번호. makerId 가 null 이면 전체에서 찾는다 */
    Integer findLatestProjectId(@Param("makerId") Integer makerId,
                                @Param("approvalStatus") String approvalStatus);

    // ===== B: 프로젝트 보기 · 필터링 (검사 06·07) =====
    /** ★ MyBatis 동적 쿼리: 조건이 있는 것만 WHERE 에 붙는다 */
    List<ProjectSummary> search(ProjectSearchCondition condition);

    // ===== D: 메이커 펀딩 현황 (검사 01) =====
    /** ★ 3단 중첩 resultMap: 프로젝트 → 리워드 → 서포터. projectId 가 null 이면 메이커의 모든 프로젝트 */
    List<FundingStatus> findFundingStatus(@Param("makerId") int makerId,
                                          @Param("projectId") Integer projectId);
}
