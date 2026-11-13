package ChefBot_Telegram.dto;

public record AuthRequestDTO(
    String email,
    String password,
    String name
) {}
