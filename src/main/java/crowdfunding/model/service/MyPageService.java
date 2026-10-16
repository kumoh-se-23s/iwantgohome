package crowdfunding.model.service;

import crowdfunding.config.AppConfig;
import crowdfunding.exception.BusinessException;
import crowdfunding.model.dao.SupporterMapper;
import crowdfunding.model.dao.UserMapper;
import crowdfunding.model.domain.Supporter;
import crowdfunding.model.domain.User;
import crowdfunding.model.dto.MyInfoForm;
import crowdfunding.model.enums.Role;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.util.regex.Pattern;

/**
 * MyPage 개인정보 수정
 *
 *   [검사 5] updateMyInfo : 이름 · 연락처 · 비밀번호(user) + 배송지 · 생년월일(supporter)
 *            → 두 테이블을 한 트랜잭션으로 수정 (하나만 바뀌고 하나는 실패하는 일이 없도록)
 *
 * 입력 규칙은 DB CHECK 제약과 같게 맞췄다.
 *   이름 2~10자, 연락처 010-0000-0000, 비밀번호 8~16자
 *
 * [담당] D (검사 05)
 */
@RequiredArgsConstructor
public class MyPageService {

    private final SqlSessionFactory sqlSessionFactory;   // Main 에서 생성자로 넣어 준다

    private static final Pattern PHONE = Pattern.compile("^010-\\d{4}-\\d{4}$");

    /** [검사 5] 개인정보 수정. form 에서 null 인 항목은 바꾸지 않는다. */
    public void updateMyInfo(int userId, MyInfoForm form) {
        if (form == null) {
            throw new BusinessException("수정할 내용이 없습니다.");
        }
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            try {
                UserMapper userMapper = session.getMapper(UserMapper.class);
                SupporterMapper supporterMapper = session.getMapper(SupporterMapper.class);

                User user = ServiceGuard.requireUser(session, userId, Role.SUPPORTER);
                Supporter supporter = supporterMapper.findByUserId(userId);
                if (supporter == null) {
                    throw new BusinessException("서포터 프로필이 없는 사용자입니다. (user_id=" + userId + ")");
                }

                // 1) user 테이블: 이름 · 연락처 · 비밀번호
                if (form.getUserName() != null) {
                    String name = form.getUserName().trim();
                    if (name.length() < 2 || name.length() > 10) {
                        throw new BusinessException("이름은 2~10자로 입력해 주세요.");
                    }
                    user.setUserName(name);
                }
                if (form.getPhoneNumber() != null) {
                    String phone = form.getPhoneNumber().trim();
                    if (!PHONE.matcher(phone).matches()) {
                        throw new BusinessException("연락처는 010-0000-0000 형식으로 입력해 주세요.");
                    }
                    user.setPhoneNumber(phone);
                }
                if (form.getNewPassword() != null) {
                    // 비밀번호를 바꿀 때는 현재 비밀번호를 한 번 더 확인한다
                    if (!user.getPw().equals(form.getCurrentPassword())) {
                        throw new BusinessException("현재 비밀번호가 일치하지 않습니다.");
                    }
                    String pw = form.getNewPassword();
                    if (pw.length() < 8 || pw.length() > 16) {
                        throw new BusinessException("새 비밀번호는 8~16자로 입력해 주세요.");
                    }
                    user.setPw(pw);
                }

                // 2) supporter 테이블: 배송지 · 생년월일
                if (form.getDeliveryAddress() != null) {
                    String address = form.getDeliveryAddress().trim();
                    if (address.isEmpty() || address.length() > 255) {
                        throw new BusinessException("배송지는 1~255자로 입력해 주세요.");
                    }
                    supporter.setDeliveryAddress(address);
                }
                if (form.getBirthDate() != null) {
                    if (form.getBirthDate().isAfter(AppConfig.today())) {
                        throw new BusinessException("생년월일이 오늘 이후일 수 없습니다.");
                    }
                    supporter.setBirthDate(form.getBirthDate());
                }

                userMapper.updateProfile(user);
                supporterMapper.update(supporter);
                session.commit();   // 두 테이블이 함께 반영된다
            } catch (RuntimeException e) {
                session.rollback();
                throw e;
            }
        }
    }

    // ---------------------------------------------------------------- 조회

    public User findUser(int userId) {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            return session.getMapper(UserMapper.class).findById(userId);
        }
    }

    public Supporter findSupporter(int userId) {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            return session.getMapper(SupporterMapper.class).findByUserId(userId);
        }
    }
}
