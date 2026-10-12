package crowdfunding.model.service;

import java.util.regex.Pattern;

/**
 * 입력값 검사 도우미. 규칙은 crowdfunding.sql 의 CHECK 제약조건과 같다.
 * DB 까지 가기 전에 서비스에서 먼저 검사해서 알기 쉬운 메시지를 돌려준다.
 */
public final class Validator {

    private static final Pattern PHONE = Pattern.compile("^010-[0-9]{4}-[0-9]{4}$");   // ck_user_phone

    private Validator() {
    }

    /** 조건이 거짓이면 message 로 BusinessException */
    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(message);
        }
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** 앞뒤 공백을 제거하고, 비었거나 길이가 범위를 벗어나면 예외 */
    public static String requireText(String value, String fieldName, int min, int max) {
        require(!isBlank(value), withJosa(fieldName, "을", "를") + " 입력하세요.");
        String trimmed = value.trim();
        require(trimmed.length() >= min && trimmed.length() <= max,
                withJosa(fieldName, "은", "는") + " " + min + "~" + max + "자로 입력하세요. (현재 " + trimmed.length() + "자)");
        return trimmed;
    }

    /** 선택 입력: 비어 있으면 null, 있으면 최대 길이 검사 */
    public static String optionalText(String value, String fieldName, int max) {
        if (isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        require(trimmed.length() <= max, withJosa(fieldName, "은", "는") + " 최대 " + max + "자까지 입력할 수 있습니다.");
        return trimmed;
    }

    /** 받침 유무에 따라 조사를 붙인다. 예) 이름 → "이름은", 배송지 → "배송지는" */
    private static String withJosa(String word, String withBatchim, String withoutBatchim) {
        char last = word.charAt(word.length() - 1);
        boolean batchim = last >= 0xAC00 && last <= 0xD7A3 && (last - 0xAC00) % 28 != 0;
        return word + (batchim ? withBatchim : withoutBatchim);
    }

    public static String requirePhone(String phone) {
        require(phone != null && PHONE.matcher(phone.trim()).matches(), "연락처는 010-0000-0000 형식으로 입력하세요.");
        return phone.trim();
    }

    /** 비밀번호 8~16자 (ck_user_pw_len). 공백은 허용하지 않음 */
    public static String requirePassword(String password) {
        require(password != null && !password.contains(" "), "비밀번호에는 공백을 쓸 수 없습니다.");
        require(password.length() >= 8 && password.length() <= 16, "비밀번호는 8~16자로 입력하세요.");
        return password;
    }
}
