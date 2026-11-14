package ChefBot_Telegram.dto;

public record UserProfileDTO(
    String username,
    String name,
    long recipesCount,
    long followersCount,
    long followingCount,
    boolean isFollowing,
    boolean isSelf
) {}
