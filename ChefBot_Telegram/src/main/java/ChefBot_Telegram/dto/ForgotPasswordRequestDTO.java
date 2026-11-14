package ChefBot_Telegram.dto;

public record ForgotPasswordRequestDTO(
    String identifier // pode ser email ou username
) {}
