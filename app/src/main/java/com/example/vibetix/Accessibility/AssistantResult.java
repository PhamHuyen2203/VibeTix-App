package com.example.vibetix.Accessibility;

/**
 * AssistantResult — kết quả sau khi thực hiện một AssistantCommand.
 * message sẽ được TTS đọc lên, hapticType quyết định kiểu rung phản hồi.
 */
public class AssistantResult {

    public enum HapticType { SUCCESS, ERROR, NONE }

    private final boolean success;
    private final String message;
    private final HapticType hapticType;

    private AssistantResult(boolean success, String message, HapticType hapticType) {
        this.success = success;
        this.message = message;
        this.hapticType = hapticType;
    }

    public static AssistantResult ok(String message) {
        return new AssistantResult(true, message, HapticType.SUCCESS);
    }

    public static AssistantResult error(String message) {
        return new AssistantResult(false, message, HapticType.ERROR);
    }

    /** Kết quả thành công nhưng không cần rung (ví dụ lệnh STOP). */
    public static AssistantResult silent(String message) {
        return new AssistantResult(true, message, HapticType.NONE);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public HapticType getHapticType() { return hapticType; }
}
