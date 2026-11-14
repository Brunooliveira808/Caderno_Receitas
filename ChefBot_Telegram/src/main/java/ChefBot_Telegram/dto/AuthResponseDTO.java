package ChefBot_Telegram.dto;

public record AuthResponseDTO(
    String token,
    Long id,
    String email,
    String username,
    String name,
    Long telegramChatId
) {}
