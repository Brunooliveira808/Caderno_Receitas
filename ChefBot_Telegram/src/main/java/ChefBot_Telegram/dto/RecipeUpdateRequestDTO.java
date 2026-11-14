package ChefBot_Telegram.dto;

import java.util.List;

public record RecipeUpdateRequestDTO(
    String title,
    String category,
    String sourceAuthor,
    Integer prepTimeMinutes,
    Integer portions,
    Boolean isPublic,
    List<String> tags,
    List<IngredientDTO> ingredients,
    List<InstructionDTO> instructions
) {}
