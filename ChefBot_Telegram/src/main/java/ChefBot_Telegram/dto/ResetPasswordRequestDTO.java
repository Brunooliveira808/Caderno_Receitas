package ChefBot_Telegram.dto;

public record ResetPasswordRequestDTO(
    String token,
    String newPassword,
    String confirmPassword
) {}
