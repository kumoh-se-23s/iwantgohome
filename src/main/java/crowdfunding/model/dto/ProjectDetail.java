package crowdfunding.model.dto;

import java.io.Serializable;
import java.util.List;

/** 프로젝트 상세 보기 = 요약 정보 + 설명 + 리워드 목록(잔여 수량) + 로그인한 서포터 본인의 후원/좋아요 여부 */
public class ProjectDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    private ProjectSummary summary;
    private String projectDescription;
    private List<RewardView> rewards;
    private Integer myRewardId;     // 로그인한 서포터가 이 프로젝트에 신청한 리워드 (없으면 null)
    private boolean likedByMe;      // 로그인한 서포터가 좋아요를 눌렀는가

    public ProjectSummary getSummary() { return summary; }
    public void setSummary(ProjectSummary summary) { this.summary = summary; }
    public String getProjectDescription() { return projectDescription; }
    public void setProjectDescription(String projectDescription) { this.projectDescription = projectDescription; }
    public List<RewardView> getRewards() { return rewards; }
    public void setRewards(List<RewardView> rewards) { this.rewards = rewards; }
    public Integer getMyRewardId() { return myRewardId; }
    public void setMyRewardId(Integer myRewardId) { this.myRewardId = myRewardId; }
    public boolean isLikedByMe() { return likedByMe; }
    public void setLikedByMe(boolean likedByMe) { this.likedByMe = likedByMe; }
}
