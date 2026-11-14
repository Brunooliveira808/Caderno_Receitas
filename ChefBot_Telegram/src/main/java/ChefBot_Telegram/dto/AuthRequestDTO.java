package ChefBot_Telegram.dto;

public record AuthRequestDTO(
    String email,
    String username,
    String password,
    String confirmPassword,
    String name
) {
    public AuthRequestDTO(String email, String password, String name) {
        this(email, (email != null && email.contains("@")) ? email.substring(0, email.indexOf('@')).replaceAll("[^a-zA-Z0-9_.]", "") : null, password, password, name);
    }
}
