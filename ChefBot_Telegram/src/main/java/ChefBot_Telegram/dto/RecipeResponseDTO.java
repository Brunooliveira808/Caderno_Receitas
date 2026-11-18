package ChefBot_Telegram.dto;

import java.util.List;

public record RecipeResponseDTO(
    Boolean isRecipe,
    String refusalReason,
    String title,
    String category,
    String sourceAuthor,
    Integer prepTimeMinutes,
    Integer portions,
    String transcription,
    List<IngredientDTO> ingredients,
    List<InstructionDTO> instructions
) {
    public RecipeResponseDTO(
        Boolean isRecipe,
        String refusalReason,
        String title,
        String category,
        String sourceAuthor,
        Integer prepTimeMinutes,
        Integer portions,
        List<IngredientDTO> ingredients,
        List<InstructionDTO> instructions
    ) {
        this(isRecipe, refusalReason, title, category, sourceAuthor, prepTimeMinutes, portions, null, ingredients, instructions);
    }
}
