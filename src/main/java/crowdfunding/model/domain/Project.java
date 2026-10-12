package crowdfunding.model.domain;

import crowdfunding.model.enums.ApprovalStatus;
import crowdfunding.model.enums.ProjectStatus;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * project 테이블 1행.
 * 메이커가 프로젝트를 등록/수정할 때 입력 데이터(폼)로도 그대로 사용한다.
 * (승인 상태, 메이커 번호 같은 값은 클라이언트가 보내도 서버의 MakerService 가 덮어쓴다.)
 */
public class Project implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer projectId;
    private String projectName;
    private String projectDescription;
    private ApprovalStatus approvalStatus;   // draft / waiting / approved / reject
    private String rejectReason;             // 거절 사유 (reject 일 때 필수)
    private ProjectStatus projectStatus;     // 승인 전 NULL → in_progress → success / fail
    private LocalDate startDate;
    private LocalDate endDate;
    private int targetAmount;
    private LocalDateTime createdAt;
    private Integer makerId;                 // DB 컬럼명은 user_id (프로젝트를 만든 메이커)
    private Integer categoryId;

    public Project() {
    }

    /** 메이커가 새 프로젝트를 등록할 때 쓰는 생성자 */
    public Project(String projectName, String projectDescription, int categoryId,
                   int targetAmount, LocalDate startDate, LocalDate endDate) {
        this.projectName = projectName;
        this.projectDescription = projectDescription;
        this.categoryId = categoryId;
        this.targetAmount = targetAmount;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getProjectDescription() { return projectDescription; }
    public void setProjectDescription(String projectDescription) { this.projectDescription = projectDescription; }
    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(ApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public ProjectStatus getProjectStatus() { return projectStatus; }
    public void setProjectStatus(ProjectStatus projectStatus) { this.projectStatus = projectStatus; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public int getTargetAmount() { return targetAmount; }
    public void setTargetAmount(int targetAmount) { this.targetAmount = targetAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Integer getMakerId() { return makerId; }
    public void setMakerId(Integer makerId) { this.makerId = makerId; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
}
