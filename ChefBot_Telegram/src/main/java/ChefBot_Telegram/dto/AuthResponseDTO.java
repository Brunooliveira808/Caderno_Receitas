package ChefBot_Telegram.dto;

public record AuthResponseDTO(
    String token,
    Long id,
    String email,
    String name,
    Long telegramChatId
) {}
