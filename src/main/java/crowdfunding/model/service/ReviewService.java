package crowdfunding.model.service;

import crowdfunding.config.AppConfig;
import crowdfunding.exception.BusinessException;
import crowdfunding.exception.DbErrors;
import crowdfunding.model.dao.FundingMapper;
import crowdfunding.model.dao.ReviewMapper;
import crowdfunding.model.domain.Project;
import crowdfunding.model.domain.Review;
import crowdfunding.model.dto.ReviewView;
import crowdfunding.model.enums.ProjectStatus;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.time.LocalDate;

/**
 * 리뷰와 별점
 *
 *   [검사 9] writeReview : 마감된 프로젝트 + 펀딩이 성공한 프로젝트 + 본인이 후원한 프로젝트에만, 1회
 *            findMyReview : 리워드명 / 별점 / 리뷰 출력용
 *
 * (펀딩 성공 = 리워드가 배송 완료된 것으로 간주 - 과제 4.4, 4.5)
 *
 * [담당] C (검사 09)
 */
@RequiredArgsConstructor
public class ReviewService {

    private static final int MAX_CONTENT_LENGTH = 1000;

    private final SqlSessionFactory sqlSessionFactory;   // Main 에서 생성자로 넣어 준다
    private final ProjectCloseService closeService;

    /** @return 새로 만들어진 review_id */
    public int writeReview(int userId, int projectId, int rating, String content) {
        closeService.closeExpiredProjects();   // 기간이 막 끝난 프로젝트도 성공/실패가 정해지도록
        LocalDate today = AppConfig.today();

        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            try {
                ReviewMapper reviewMapper = session.getMapper(ReviewMapper.class);

                if (rating < 1 || rating > 5) {
                    throw new BusinessException("별점은 1~5점만 줄 수 있습니다.");
                }
                String text = content == null ? null : content.trim();
                if (text != null && text.length() > MAX_CONTENT_LENGTH) {
                    throw new BusinessException("리뷰는 " + MAX_CONTENT_LENGTH + "자 이하로 작성해 주세요.");
                }

                Project project = ServiceGuard.requireProject(session, projectId);

                // 1) 본인이 후원한 프로젝트인지
                if (!session.getMapper(FundingMapper.class).exists(userId, projectId)) {
                    throw new BusinessException("후원한 프로젝트에만 리뷰를 작성할 수 있습니다.");
                }
                // 2) 마감된 프로젝트인지
                if (!today.isAfter(project.getEndDate())) {
                    throw new BusinessException("아직 마감되지 않은 프로젝트입니다. (종료일 " + project.getEndDate() + ")");
                }
                // 3) 펀딩에 성공한 프로젝트인지
                if (!ProjectStatus.SUCCESS.matches(project.getProjectStatus())) {
                    throw new BusinessException("펀딩에 성공한 프로젝트에만 리뷰를 작성할 수 있습니다. (현재: "
                            + ProjectStatus.labelOf(project.getProjectStatus()) + ")");
                }
                // 4) 프로젝트당 리뷰 1개
                if (reviewMapper.exists(userId, projectId)) {
                    throw new BusinessException("이미 리뷰를 작성한 프로젝트입니다. (프로젝트당 1회)");
                }

                Review review = new Review();
                review.setUserId(userId);
                review.setProjectId(projectId);
                review.setRating(rating);
                review.setReviewContent(text);
                try {
                    reviewMapper.insert(review);
                } catch (PersistenceException e) {
                    if (DbErrors.isDuplicateKey(e)) {
                        throw new BusinessException("이미 리뷰를 작성한 프로젝트입니다. (프로젝트당 1회)");
                    }
                    throw e;
                }
                session.commit();
                return review.getReviewId();
            } catch (RuntimeException e) {
                session.rollback();
                throw e;
            }
        }
    }

    /** 리워드명 / 별점 / 리뷰 (없으면 null) */
    public ReviewView findMyReview(int userId, int projectId) {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            return session.getMapper(ReviewMapper.class).findView(userId, projectId);
        }
    }
}
