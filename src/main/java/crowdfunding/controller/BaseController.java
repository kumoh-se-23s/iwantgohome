package crowdfunding.controller;

import crowdfunding.model.dto.LoginUser;
import crowdfunding.model.enums.Role;
import crowdfunding.model.service.BusinessException;
import crowdfunding.protocol.Response;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * 모든 컨트롤러의 공통 부분.
 * <ul>
 *   <li>권한 검사 — 로그인한 사용자 유형에 따라 이용 가능한 기능을 서버에서 구분</li>
 *   <li>예외 → Response 변환 — 서비스가 던진 예외가 서버(접속 스레드)를 죽이지 않게 한다</li>
 * </ul>
 */
public abstract class BaseController {

    protected <T> Response<T> handle(Supplier<Response<T>> action) {
        try {
            return action.get();
        } catch (BusinessException e) {
            return Response.fail(e.getMessage());                 // 업무 규칙 위반 → 사용자에게 메시지
        } catch (RuntimeException e) {
            System.err.println("[서버 오류] " + e);                 // 예상하지 못한 오류 → 로그 남기고 실패 응답
            e.printStackTrace();
            return Response.fail("서버 내부 오류가 발생했습니다. (" + e.getClass().getSimpleName() + ")");
        }
    }

    protected void requireRole(LoginUser user, Role... allowed) {
        if (user == null) {
            throw new BusinessException("로그인이 필요합니다.");
        }
        if (allowed.length > 0 && Arrays.stream(allowed).noneMatch(r -> r == user.getRole())) {
            throw new BusinessException(user.getRole().getLabel() + " 계정으로는 사용할 수 없는 기능입니다.");
        }
    }
}
