package crowdfunding.model.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * funding(신청 현황) 테이블 1행.
 * PK 가 (user_id, project_id) 이므로 한 서포터는 한 프로젝트에 한 번만 후원할 수 있다.
 * 서포터의 리워드 신청 요청 데이터(어느 프로젝트의 어느 리워드?)로도 사용.
 */
public class Funding implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer userId;          // 서포터
    private Integer projectId;
    private Integer rewardId;
    private LocalDateTime fundedDate; // 신청일 (DB 기본값 CURRENT_TIMESTAMP)

    public Funding() {
    }

    /** 서포터의 리워드 신청 요청 */
    public Funding(int projectId, int rewardId) {
        this.projectId = projectId;
        this.rewardId = rewardId;
    }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Integer getProjectId() { return projectId; }
    public void setProjectId(Integer projectId) { this.projectId = projectId; }
    public Integer getRewardId() { return rewardId; }
    public void setRewardId(Integer rewardId) { this.rewardId = rewardId; }
    public LocalDateTime getFundedDate() { return fundedDate; }
    public void setFundedDate(LocalDateTime fundedDate) { this.fundedDate = fundedDate; }
}
