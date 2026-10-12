package crowdfunding.view;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * [2차] 콘솔 입력 도우미. 잘못 입력하면 다시 물어본다.
 * 한 줄에 하나씩 읽으므로, 시연용 입력을 미리 준비해 여러 줄을 복사·붙여넣기 해도 순서대로 처리된다.
 */
public class ConsoleInput {

    private final Scanner scanner = new Scanner(System.in);

    /** 한 줄 그대로 (앞뒤 공백 제거) */
    public String line(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new IllegalStateException("입력이 종료되었습니다.");
        }
        return scanner.nextLine().trim();
    }

    /** 반드시 입력 */
    public String text(String prompt) {
        while (true) {
            String value = line(prompt);
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("  값을 입력하세요.");
        }
    }

    /** 엔터만 치면 null */
    public String optionalText(String prompt) {
        String value = line(prompt);
        return value.isEmpty() ? null : value;
    }

    /** 정수 (쉼표·'원' 허용: 39,000원 → 39000) */
    public int number(String prompt) {
        while (true) {
            Integer value = parseInt(line(prompt));
            if (value != null) {
                return value;
            }
            System.out.println("  숫자를 입력하세요.");
        }
    }

    /** 엔터만 치면 null */
    public Integer optionalNumber(String prompt) {
        while (true) {
            String raw = line(prompt);
            if (raw.isEmpty()) {
                return null;
            }
            Integer value = parseInt(raw);
            if (value != null) {
                return value;
            }
            System.out.println("  숫자를 입력하세요. (건너뛰려면 엔터)");
        }
    }

    public int numberInRange(String prompt, int min, int max) {
        while (true) {
            int value = number(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.println("  " + min + " ~ " + max + " 사이의 숫자를 입력하세요.");
        }
    }

    /** yyyy-MM-dd, 엔터만 치면 defaultValue */
    public LocalDate date(String prompt, LocalDate defaultValue) {
        while (true) {
            String raw = line(prompt);
            if (raw.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
            try {
                return LocalDate.parse(raw);
            } catch (DateTimeParseException e) {
                System.out.println("  날짜는 2026-11-02 형식으로 입력하세요.");
            }
        }
    }

    /** yyyy-MM-dd, 엔터만 치면 null */
    public LocalDate optionalDate(String prompt) {
        while (true) {
            String raw = line(prompt);
            if (raw.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(raw);
            } catch (DateTimeParseException e) {
                System.out.println("  날짜는 2026-11-02 형식으로 입력하세요. (건너뛰려면 엔터)");
            }
        }
    }

    public boolean confirm(String prompt) {
        while (true) {
            String value = line(prompt + " (y/n): ").toLowerCase();
            if (value.equals("y") || value.equals("yes")) return true;
            if (value.equals("n") || value.equals("no")) return false;
            System.out.println("  y 또는 n 을 입력하세요.");
        }
    }

    private static Integer parseInt(String raw) {
        try {
            return Integer.parseInt(raw.replace(",", "").replace("원", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
