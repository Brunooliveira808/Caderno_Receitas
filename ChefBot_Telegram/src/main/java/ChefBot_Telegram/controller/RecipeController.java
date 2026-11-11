package ChefBot_Telegram.controller;

import ChefBot_Telegram.dto.IngredientDTO;
import ChefBot_Telegram.dto.InstructionDTO;
import ChefBot_Telegram.dto.RecipeResponseDTO;
import ChefBot_Telegram.dto.RecipeUpdateRequestDTO;
import ChefBot_Telegram.dto.RecipeWebDTO;
import ChefBot_Telegram.model.Recipe;
import ChefBot_Telegram.service.GeminiService;
import ChefBot_Telegram.service.RecipeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recipes")
@CrossOrigin(origins = "*")
public class RecipeController {

    private static final Logger log = LoggerFactory.getLogger(RecipeController.class);

    private final RecipeService recipeService;
    private final GeminiService geminiService;

    // ChatId padrão para operações originadas na Web
    private static final Long WEB_DEFAULT_CHAT_ID = 999999999L;
    private static final String WEB_DEFAULT_USER_NAME = "Web Chef";

    public RecipeController(RecipeService recipeService, GeminiService geminiService) {
        this.recipeService = recipeService;
        this.geminiService = geminiService;
    }

    /**
     * Lista todas as receitas ou filtra por categoria.
     */
    @GetMapping
    public List<RecipeWebDTO> getAllRecipes(@RequestParam(required = false) String category) {
        List<Recipe> recipes = recipeService.findAllRecipes();

        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("todos")) {
            String normCategory = recipeService.normalizeText(category);
            recipes = recipes.stream()
                .filter(r -> r.getCategory() != null && recipeService.normalizeText(r.getCategory()).contains(normCategory))
                .collect(Collectors.toList());
        }

        return recipes.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Detalha uma receita por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecipeWebDTO> getRecipeById(@PathVariable Long id) {
        return recipeService.findRecipeById(id)
            .map(recipe -> ResponseEntity.ok(toDTO(recipe)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Cria receita a partir de texto (usando a IA Gemini para estruturar).
     */
    @PostMapping("/from-text")
    public ResponseEntity<?> createFromText(@RequestBody Map<String, String> payload) {
        String text = payload.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Texto não informado."));
        }

        try {
            RecipeResponseDTO dto = geminiService.extractRecipeFromText(text);
            if (Boolean.FALSE.equals(dto.isRecipe())) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", dto.refusalReason() != null ? dto.refusalReason() : "Não foi detectada uma receita culinária válida."));
            }

            Recipe saved = recipeService.saveRecipeFromDTO(WEB_DEFAULT_CHAT_ID, WEB_DEFAULT_USER_NAME, dto, text);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
        } catch (Exception e) {
            log.error("Erro ao processar receita por texto: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro ao processar com a IA: " + e.getMessage()));
        }
    }

    /**
     * Cria receita a partir de gravação de áudio do microfone.
     */
    @PostMapping(value = "/from-audio", consumes = "multipart/form-data")
    public ResponseEntity<?> createFromAudio(@RequestParam("audio") MultipartFile audioFile) {
        if (audioFile.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Arquivo de áudio vazio."));
        }

        try {
            byte[] bytes = audioFile.getBytes();
            String mimeType = audioFile.getContentType() != null ? audioFile.getContentType() : "audio/webm";

            RecipeResponseDTO dto = geminiService.extractRecipeFromAudio(bytes, mimeType);
            if (Boolean.FALSE.equals(dto.isRecipe())) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", dto.refusalReason() != null ? dto.refusalReason() : "Não foi identificada uma receita culinária no áudio."));
            }

            String transcriptToSave = (dto.transcription() != null && !dto.transcription().isBlank())
                ? dto.transcription()
                : "Gravação de áudio Web";

            Recipe saved = recipeService.saveRecipeFromDTO(WEB_DEFAULT_CHAT_ID, WEB_DEFAULT_USER_NAME, dto, transcriptToSave);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
        } catch (Exception e) {
            log.error("Erro ao processar receita por áudio: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro ao processar áudio com a IA: " + e.getMessage()));
        }
    }

    /**
     * Atualiza uma receita existente usando instruções em linguagem natural via IA.
     */
    @PostMapping("/{id}/ai-update")
    public ResponseEntity<?> updateRecipeWithAi(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String instructions = payload.get("instructions");
        if (instructions == null || instructions.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Instruções de alteração não informadas."));
        }

        Optional<Recipe> existingOpt = recipeService.findRecipeById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Recipe existing = existingOpt.get();
            RecipeResponseDTO updatedDto = geminiService.updateRecipe(existing, instructions);
            Long ownerChatId = existing.getUser() != null ? existing.getUser().getTelegramChatId() : WEB_DEFAULT_CHAT_ID;
            Recipe updated = recipeService.updateRecipeFromDTO(ownerChatId, id, updatedDto);
            return ResponseEntity.ok(toDTO(updated));
        } catch (Exception e) {
            log.error("Erro ao atualizar receita com IA: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro ao atualizar com IA: " + e.getMessage()));
        }
    }

    /**
     * Atualiza uma receita manualmente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRecipe(@PathVariable Long id, @RequestBody RecipeUpdateRequestDTO dto) {
        try {
            Recipe updated = recipeService.updateRecipeManual(id, dto);
            return ResponseEntity.ok(toDTO(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erro ao atualizar receita: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Exclui uma receita.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRecipe(@PathVariable Long id) {
        boolean deleted = recipeService.deleteRecipeById(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private RecipeWebDTO toDTO(Recipe recipe) {
        List<IngredientDTO> ings = recipe.getIngredients().stream()
            .map(i -> new IngredientDTO(i.getName(), i.getQuantity(), i.getUnit(), i.getNotes()))
            .collect(Collectors.toList());

        List<InstructionDTO> insts = recipe.getInstructions().stream()
            .map(i -> new InstructionDTO(i.getStepNumber(), i.getInstructionText()))
            .collect(Collectors.toList());

        return new RecipeWebDTO(
            recipe.getId(),
            recipe.getTitle(),
            recipe.getCategory(),
            recipe.getSourceAuthor(),
            recipe.getPrepTimeMinutes(),
            recipe.getPortions(),
            recipe.getRawTranscript(),
            ings,
            insts
        );
    }
}
