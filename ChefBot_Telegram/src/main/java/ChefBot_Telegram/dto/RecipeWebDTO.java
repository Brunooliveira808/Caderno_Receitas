package ChefBot_Telegram.dto;

import java.util.List;

public record RecipeWebDTO(
    Long id,
    String title,
    String category,
    String sourceAuthor,
    Integer prepTimeMinutes,
    Integer portions,
    String rawTranscript,
    Boolean isPublic,
    String authorUsername,
    String forkedFromUsername,
    Long forkCount,
    List<String> tags,
    List<IngredientDTO> ingredients,
    List<InstructionDTO> instructions
) {}
