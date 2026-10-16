package crowdfunding.model.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * project 테이블 한 행.
 *
 * approvalStatus / projectStatus 는 DB 값(소문자 문자열) 그대로 담는다.
 * 비교는 enum 으로:  ApprovalStatus.APPROVED.matches(project.getApprovalStatus())
 *
 * [담당] 공통 기반
 */
@Getter
@Setter
@ToString
public class Project {

    private int projectId;               // project_id          PK
    private String projectName;          // project_name
    private String projectDescription;   // project_description (NULL 가능)
    private String approvalStatus;       // approval_status     draft / waiting / approved / reject
    private String rejectReason;         // reject_reason       거절 사유
    private String projectStatus;        // project_status      in_progress / success / fail (승인 전 NULL)
    private LocalDate startDate;         // start_date
    private LocalDate endDate;           // end_date
    private int targetAmount;            // target_amount       목표 금액
    private LocalDateTime createdAt;     // created_at
    private int userId;                  // user_id             메이커
    private int categoryId;              // category_id

    /** 이 메이커가 만든 프로젝트인지 */
    public boolean isOwnedBy(int makerId) {
        return userId == makerId;
    }
}
