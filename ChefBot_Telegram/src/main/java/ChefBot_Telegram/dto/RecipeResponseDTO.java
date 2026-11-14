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
    Boolean isPublic,
    String authorUsername,
    String forkedFromUsername,
    Long forkCount,
    List<String> tags,
    List<IngredientDTO> ingredients,
    List<InstructionDTO> instructions
) {
    // Construtor com transcription (legado intermediário)
    public RecipeResponseDTO(
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
        this(isRecipe, refusalReason, title, category, sourceAuthor, prepTimeMinutes, portions, transcription, true, null, null, 0L, List.of(), ingredients, instructions);
    }

    // Construtor de compatibilidade para código e testes existentes sem transcription nem campos sociais
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
        this(isRecipe, refusalReason, title, category, sourceAuthor, prepTimeMinutes, portions, null, true, null, null, 0L, List.of(), ingredients, instructions);
    }
}


