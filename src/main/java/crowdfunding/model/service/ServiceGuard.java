package crowdfunding.model.service;

import crowdfunding.exception.BusinessException;
import crowdfunding.model.dao.ProjectMapper;
import crowdfunding.model.dao.UserMapper;
import crowdfunding.model.domain.Project;
import crowdfunding.model.domain.User;
import crowdfunding.model.enums.Role;
import org.apache.ibatis.session.SqlSession;

/**
 * 여러 Service 가 공통으로 쓰는 확인 작업 (권한, 존재 여부, 본인 프로젝트인지).
 * 같은 패키지의 Service 만 쓰도록 public 을 붙이지 않았다.
 *
 * [담당] 공통 기반
 */
final class ServiceGuard {

    private ServiceGuard() {
    }

    /** 사용자가 있고, 역할이 role 인지 확인 */
    static User requireUser(SqlSession session, int userId, Role role) {
        User user = session.getMapper(UserMapper.class).findById(userId);
        if (user == null) {
            throw new BusinessException("존재하지 않는 사용자입니다. (user_id=" + userId + ")");
        }
        if (!role.matches(user.getRole())) {
            throw new BusinessException(role.getLabel() + "만 사용할 수 있는 기능입니다. (user_id=" + userId
                    + " 은(는) " + Role.labelOf(user.getRole()) + ")");
        }
        return user;
    }

    /** 프로젝트가 있는지 확인 */
    static Project requireProject(SqlSession session, int projectId) {
        Project project = session.getMapper(ProjectMapper.class).findById(projectId);
        if (project == null) {
            throw new BusinessException("존재하지 않는 프로젝트입니다. (project_id=" + projectId + ")");
        }
        return project;
    }

    /** 이 메이커가 만든 프로젝트인지 확인 */
    static Project requireOwnProject(SqlSession session, int makerId, int projectId) {
        Project project = requireProject(session, projectId);
        if (!project.isOwnedBy(makerId)) {
            throw new BusinessException("본인이 만든 프로젝트가 아닙니다. (project_id=" + projectId + ")");
        }
        return project;
    }

    static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
