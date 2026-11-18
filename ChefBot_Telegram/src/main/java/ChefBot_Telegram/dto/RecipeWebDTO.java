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
    String authorUsername,
    List<IngredientDTO> ingredients,
    List<InstructionDTO> instructions
) {}
